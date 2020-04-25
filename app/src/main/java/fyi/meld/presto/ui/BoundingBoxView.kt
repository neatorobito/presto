package fyi.meld.presto.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.util.TypedValue
import android.view.View
import fyi.meld.presto.utils.BoundingBox
import fyi.meld.presto.utils.Constants


class BoundingBoxView(
    context: Context?
) :
    View(context) {
    private var newBoxes: List<BoundingBox>? = null
    private var drawnBoxes: List<BoundingBox>? = null
    private val fgPaint: Paint
    private val colors = intArrayOf(
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
        Color.BLUE,
        Color.RED,
        Color.GREEN
    )

    fun setNewBoxes(newBoxes: List<BoundingBox>) {
        synchronized(this) { this.newBoxes = newBoxes }
        postInvalidate()
    }

    private fun drawNewBoxes(canvas: Canvas, newBoxes: List<BoundingBox>?) {
        if(newBoxes != null)
        {
            for (boundingBox in newBoxes) {
                val label = boundingBox.classIdentifier
                val classColor = colors[boundingBox.classIndex % colors.size]

                // Get bounding box coords
                var location: RectF = boundingBox.location!!
                location.left *= width.toFloat()
                location.top *= height.toFloat()
                location.right *= width.toFloat()
                location.bottom *= height.toFloat()

                location.left -= location.left * 0.35f
                location.right *= 1.25f

                // Draw box
                fgPaint.color = classColor
                fgPaint.style = Paint.Style.STROKE
                canvas.drawRect(location, fgPaint)

                // Draw label
                val labelBounds = RectF()
                val fm = fgPaint.fontMetrics
                labelBounds.left = location.left
                labelBounds.top =
                    location.top - (-fm.ascent + fm.descent + LABEL_VERTICAL_PADDING * 2)
                labelBounds.right =
                    location.left + (fgPaint.measureText(label) + LABEL_HORIZONTAL_PADDING * 2)
                labelBounds.bottom = location.top
                fgPaint.style = Paint.Style.FILL_AND_STROKE
                canvas.drawRect(labelBounds, fgPaint)
                fgPaint.color = Color.WHITE
                fgPaint.style = Paint.Style.FILL
                canvas.drawText(
                    label!!,
                    labelBounds.left + LABEL_HORIZONTAL_PADDING,
                    labelBounds.bottom - (fm.descent + LABEL_VERTICAL_PADDING),
                    fgPaint
                )
            }
        }
    }

    public override fun onDraw(canvas: Canvas) {

        var correctedBoxes: List<BoundingBox>?

        synchronized(this) {
            correctedBoxes = this.newBoxes
        }

        if(drawnBoxes != null && newBoxes != null && drawnBoxes!!.isNotEmpty() && newBoxes!!.isNotEmpty())
        {
            for(drawnBox in drawnBoxes!!)
            {
                for(newBox in newBoxes!!)
                {
                    if( drawnBox.location?.left!! <= newBox.location?.left!! &&
                        drawnBox.location?.right!! >= newBox.location?.left!! &&
                        drawnBox.location?.top!! >= newBox.location?.bottom!! &&
                        drawnBox.location?.bottom!! <= newBox.location?.top!!)
                    {
                        Log.d(Constants.TAG, "Attempting to draw an overlapping box.")
                    }
                }
            }
        }

        drawNewBoxes(canvas, newBoxes)
        this.drawnBoxes = correctedBoxes

    }

    companion object {
        private const val TEXT_SIZE_DIP = 16f
        private const val BOX_STROKE_WITDTH = 8f
        private const val LABEL_HORIZONTAL_PADDING = 16f
        private const val LABEL_VERTICAL_PADDING = 4f
    }

    init {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            TEXT_SIZE_DIP,
            resources.displayMetrics
        )
        fgPaint = Paint()
        fgPaint.textSize = textSizePx
        fgPaint.strokeWidth = BOX_STROKE_WITDTH
    }
}
