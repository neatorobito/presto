package fyi.meld.presto.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import com.google.android.material.transition.MaterialSharedAxis
import com.google.common.util.concurrent.ListenableFuture
import fyi.meld.presto.R
import fyi.meld.presto.models.CartItem
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.ItemType
import fyi.meld.presto.utils.PriceEngine
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.hint_bar.*
import kotlinx.android.synthetic.main.price_engine_fragment.*
import kotlinx.android.synthetic.main.price_tag_dialog.*


/**
 * A simple [Fragment] subclass.
 * Use the [PriceEngineFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class PriceEngineFragment: Fragment(), PriceEngine.DetectionStatusHandler {

    private lateinit var mCameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var mPreview : Preview
    private lateinit var mCameraSelector : CameraSelector
    private lateinit var mImageAnalysisUseCase : ImageAnalysis

    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private var prestoVM : PrestoViewModel = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backward = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        val forward = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        enterTransition = forward
        returnTransition = backward
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.price_engine_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        prestoVM.priceEngine.detectionStatusHandler = this
    }

    override fun onResume() {
        super.onResume()
        if(!prestoVM.isCameraRunning)
        {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if(prestoVM.isCameraRunning)
        {
            stopCamera()
        }
    }

    fun getRandomName() : String
    {
        val randomString = (1..6)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("");

        return randomString
    }

    private fun showPriceDiag(price : String)
    {
        var newItem = CartItem(getRandomName(), ItemType.Other, price.drop(1).toFloat())

        price_tag_diag.animate()
            .withStartAction {

                val dpFactor: Float = scanner_frame.resources.displayMetrics.density
                val width = (250 * dpFactor).toInt()
                val height = (175 * dpFactor).toInt()

                val layoutParams = FrameLayout.LayoutParams(width, height)
                layoutParams.leftMargin = (scanner_frame.width / 2) - (width / 2)
                layoutParams.topMargin = (scanner_frame.height / 2) - (height / 2)

                price_tag_diag.layoutParams = layoutParams
                scanner_frame.postInvalidate()

                price_detected_indicator.visibility = View.INVISIBLE

                price_tag_diag.visibility = View.VISIBLE
                price_text.text = String.format("$%.2f", newItem.getPriceAfterTax(8.26f))

            }
            .alpha(1.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5)
    }

    private fun configureCamera() {

        val viewFinderDisplay =  view_finder.display

        mPreview = Preview.Builder().apply {
            setTargetRotation(viewFinderDisplay.rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        }.build()

        view_finder.preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW;

        mImageAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 640))
            .build()

        mImageAnalysisUseCase.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            ImageAnalysis.Analyzer { imageProxy ->

                val displayScaleFactor: Int = scanner_frame.resources.displayMetrics.densityDpi

                prestoVM.priceEngine.findPricesAsync(imageProxy, displayScaleFactor)
                    .addOnSuccessListener {
                        Log.d(Constants.TAG, it.toString())

                        requireActivity().runOnUiThread {
                            if(price_tag_diag.visibility == View.INVISIBLE)
                            {
                                if(it.isNotBlank())
                                {
                                    hint_text.text = "Add item to cart?"
                                    showPriceDiag(it)
                                }
                                else
                                {
                                    onPriceLost()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e -> Log.e(Constants.TAG, "Price Engine encountered an error.", e) }
            }
        )

        mCameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    private fun startCamera()
    {
        scanner_frame.animate()
            .alpha(1.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION)
            .withStartAction {
                scanner_frame.visibility = View.VISIBLE

                configureCamera()

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()

                    cameraProvider.bindToLifecycle(
                        this,
                        mCameraSelector,
                        mPreview,
                        mImageAnalysisUseCase
                    )

                    mPreview.setSurfaceProvider(view_finder.createSurfaceProvider())

                }, ContextCompat.getMainExecutor(requireContext()))

                prestoVM.isCameraRunning = true
            }.start()
    }

    private fun stopCamera()
    {
        scanner_frame.animate()
            .alpha(0.0f)
            .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5)
            .withEndAction {
                scanner_frame.visibility = View.INVISIBLE

                mCameraProviderFuture.addListener(Runnable {
                    val cameraProvider = mCameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }, ContextCompat.getMainExecutor(requireContext()))

                prestoVM.isCameraRunning = false
            }.start()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceEngineFragment()
    }

    override fun onPriceLost() {
        requireActivity().runOnUiThread {
            if(price_tag_diag.visibility == View.VISIBLE)
            {
                price_tag_diag.animate()
                    .alpha(0.0f)
                    .setDuration(Constants.CAMERA_PREVIEW_FADE_DURATION / 5)
                    .withEndAction {
                        price_tag_diag.visibility = View.INVISIBLE
                        hint_text.text = "Hover over an item and its price"
                    }
            }
            else if(price_detected_indicator.visibility == View.VISIBLE)
            {
                price_detected_indicator.visibility = View.INVISIBLE
            }
        }
    }

    override fun onPriceFound() {
        requireActivity().runOnUiThread {
            if(price_tag_diag.visibility == View.INVISIBLE && price_detected_indicator.visibility == View.INVISIBLE)
            {
                price_detected_indicator.visibility = View.VISIBLE
            }
        }
    }

}