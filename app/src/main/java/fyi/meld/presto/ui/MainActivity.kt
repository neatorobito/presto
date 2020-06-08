package fyi.meld.presto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.androidisland.vita.VitaOwner
import com.androidisland.vita.vita
import fyi.meld.presto.R
import fyi.meld.presto.utils.Constants
import fyi.meld.presto.utils.PriceEngine
import fyi.meld.presto.viewmodels.PrestoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.critical_info.*
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), LifecycleOwner, MotionLayout.TransitionListener, PrestoViewModel.SwitchUIHandler {

    lateinit var prestoVM : PrestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val cartUI = CartFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, cartUI)
                .commit()
        }

//        base_container.setTransitionListener(this)

        if(!hasPermissions())
        {
            requestPermission()
        }

        configureViewModel()
    }

    override fun onNewItemUIRequested() {
        //TODO Combine this and other UI request method into a simpler solution.
        val newItemUI = NewItemFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, newItemUI)
            .addToBackStack(null)
            .commit()
    }

    override fun onCartUIRequested() {
        val cartUI = CartFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, cartUI)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else if(prestoVM.isCameraRunning)
        {
//            base_container.transitionToStart()
        }
        else {
            super.onBackPressed()
        }
    }

    private fun configureViewModel()
    {
        prestoVM = vita.with(VitaOwner.Multiple(this)).getViewModel<PrestoViewModel>()
        prestoVM.initialLayoutState = base_container.currentState
        prestoVM.switchUIHandler = this
        prestoVM.priceEngine = PriceEngine(WeakReference(this))
        prestoVM.priceEngine.initialize()

        configureDataObservers()
    }

    private fun configureDataObservers()
    {
        prestoVM.storeTrip.observe(this, Observer {
            cart_size_text.text = it.items.size.toString()
            cart_total_text.text = "$" + String.format("%.2f", it.getTotalAfterTax())
            tax_rate_text.text = String.format("%.2f", it.localTaxRate) + "%"
        })
    }

    private fun hasPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            Constants.PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_CODE) {
            if (!hasPermissions()) {
                Toast.makeText(this,
                    "Please allow the app camera permissions.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startingState: Int, endingState: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentState: Int) {

//        if(currentState != prestoVM.initialLayoutState && !prestoVM.isCameraRunning)
//        {
//            startCamera()
//        }
//        else if(currentState == prestoVM.initialLayoutState && prestoVM.isCameraRunning)
//        {
//            stopCamera()
//        }
    }
}
