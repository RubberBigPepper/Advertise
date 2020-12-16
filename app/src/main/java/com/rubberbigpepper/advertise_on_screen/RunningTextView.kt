package com.rubberbigpepper.advertise_on_screen

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toRectF
import java.lang.Math.abs


class RunningTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("", Typeface.BOLD)
        color=Color.WHITE
    }

    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        //val colorMain=Color.GRAY
        //color=  Color.argb(128,colorMain.red,colorMain.green,colorMain.blue)
        color=Color.TRANSPARENT
    }

    private var leftText = 50.0f //левый край текста, он будет смещаться каждый раз и с этого места выводиться текст
    private var textToShow = ""//текст для вывода
    private var delayToNext: Long = 10 // сколько делать задержку перед выводом следущей буквы
    private var whenCycleEnd: (()->Unit)? = null //адрес колбэка, когда закончили воспроизведение одного периода
    private var shiftHor = 2 //на сколько сдвигать текст при каждом цикле

    public var marginBottom = 0 //отступ снизу
    public var heightRect = 0//высота прямоугольника вывода

    var endCycleCallback: (()->Unit)?//адрес колбэка, когда закончили воспроизведение одного периода
        get(){
            return whenCycleEnd
        }
        set(value){
            whenCycleEnd=value
        }

    var horShift: Int
        get(){
            return shiftHor
        }
        set(value){
            shiftHor=abs(value)
        }

    var delay: Long
        get(){
            return delayToNext
        }
        set(value) {
            delayToNext=value
        }

    var text: String //а это свойство текст для вывода
        get(){
            return textToShow
        }
        set(value){
            textToShow=value
            leftText=right.toFloat()
            restartDelayedCallbacks()
        }

    var textSize: Float//размер текста
        get(){
            var metrics = DisplayMetrics()
            if (metrics!=null) {
                display.getRealMetrics(metrics)
                return paint.textSize / metrics.density
            }
            return 10.0f
        }
        set(value){
            var metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            paint.textSize=value*metrics.density
            //postInvalidate()
        }

    var textColor: Int//цвет текста
        get(){
            return paint.color
        }
        set(value){
            paint.color=value
            //postInvalidate()
        }

    var textBackground: Int//цвет текста
    get(){
        return paintBackground.color
    }
        set(value){
            paintBackground.color=value
            //postInvalidate()
        }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (textToShow.length == 0) return
        var width=this.width //ширина окна
        var y = (bottom-paddingBottom).toFloat()
        var textRect = Rect()
        paint.getTextBounds(textToShow, 0, textToShow.length, textRect)
        y = bottom - heightRect + heightRect/2 - (paint.descent() + paint.ascent()) / 2 - marginBottom
        //textRect.offset(0,y.toInt())
        //textRect.inset(-textRect.height()/5,-textRect.height()/5)
        textRect.left=0
        textRect.bottom=bottom-marginBottom
        textRect.top=textRect.bottom-heightRect
        textRect.right=width
        canvas.drawRect(textRect, paintBackground)
        /*      тут блок отрисовки фона текста, двигающегося за текстом, оказался не нужен
        //textRect.offsetTo(leftText.toInt(),textRect.top)

        canvas.drawRoundRect(textRect.toRectF(), textRect.height()*0.2f,textRect.height()*0.2f,paintBackground)
        val pathText=Path()

        //pathText.addRect(textRect.toRectF(),Path.Direction.CCW)
        //canvas.drawTextOnPath(textToShow,pathText,0.0f,0.0f, paint)*/
        canvas.drawText(textToShow, leftText, y, paint)
    }

    private fun restartDelayedCallbacks(){
        runningCallback?.let {
            handler?.removeCallbacks(it)
            handler?.postDelayed(it, delayToNext)
        }
    }

    private var runningCallback:Runnable = Runnable {
        if (textToShow.length==0)
            return@Runnable
        leftText-=shiftHor
        var textWidth=paint.measureText(textToShow)
        if (leftText+textWidth<0) {//дошли до левого края, нужно обнулиться
            leftText=right.toFloat()
            whenCycleEnd?.let { it() }//вызываем колбэк
        }
        restartDelayedCallbacks()
        invalidate()
    }
}