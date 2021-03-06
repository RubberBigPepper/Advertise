package com.android.memorymanager
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*

class AdvertiseService: Service() {
    private final val TAG = "AdvertiseService"

    companion object {
        @JvmStatic val ACTION_START = "albakm.StartAdvertiseService"
        @JvmStatic val ACTION_START_AFTERBOOT = "albakm.StartAdvertiseServiceAfterBoot"
        @JvmStatic val ACTION_STOP = "albakm.StopAdvertiseService"
        @JvmStatic val ACTION_UPDATE = "albakm.UpdateAdvertiseService"
    }
    private val m_channelId = "channel-advertise-01"
    private val m_channelName = "adevertise"
    private val m_importance = NotificationManager.IMPORTANCE_DEFAULT
    private var m_cWM: WindowManager? = null
    private var frameView: View? = null

    private var advProducer: AdvProducer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hourReceiver = TimeBroadcatReceiver()
    private var prevHour = -1
    private var prevHourAddress = -1
    private var blankStr=" "
    private var showCount=1

    private val nextAdvDataRunnable: Runnable = Runnable {//смена показа рекламы
        showNextAdvData()
    }

    private val sendMACAddressRunnable: Runnable = Runnable {//смена показа рекламы
        sendMACAddress()
    }



    override fun onBind(intent: Intent?): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
    }


    override fun onDestroy() {
        DebugLog.e(TAG, "OnDestroy reached")
        CloseChannel(this)
    }

    fun CreateChannel(cContext: Context) {
        val cNM = cContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && cNM.getNotificationChannel(m_channelId) == null) {
            val cChannel = NotificationChannel(m_channelId, m_channelName, m_importance)
            cChannel.enableVibration(false)
            cChannel.setSound(null, null)
            cNM.createNotificationChannel(cChannel)
        }
    }

    fun CloseChannel(cContext: Context) {
        val cNM = cContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && cNM.getNotificationChannel(m_channelId) != null) {
            cNM.deleteNotificationChannel(m_channelId)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CreateChannel(this)
        handleCommand(intent)
        return START_STICKY //START_REDELIVER_INTENT;
    }

    fun handleCommand(intent: Intent?) {
        val cAM = getSystemService(ALARM_SERVICE) as AlarmManager
        val cIntent = Intent()
        cIntent.action = "rubberbigpepper.AdvertiseService.NeverKillingService"
        val cPendIntent = PendingIntent.getBroadcast(this, 0, cIntent, 0)
        cAM.cancel(cPendIntent)
        try{
            unregisterReceiver(hourReceiver)
        }
        catch (ex: Exception){}
        var strAction: String? = ""
        strAction = if (intent == null) {
            DebugLog.e(TAG, "Intent is null,service was restarted. Killing")
            stopSelf()
            return
        } else {
            intent.action
        }
        if (strAction != null && strAction.equals(ACTION_STOP, ignoreCase = true)) {
            HideIndicator()
            try {
                m_cWM!!.removeView(frameView)
            } catch (ex: Exception) {
            }
            stopSelf()
        } else { //пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
            if (strAction != null && (strAction==ACTION_START||strAction==ACTION_START_AFTERBOOT)) {
                HideIndicator()
                m_cWM = null
                advProducer = AdvProducer(this)
              //  advProducer!!.readInternalFolder(this)
                showIndicator()
                if (strAction==ACTION_START_AFTERBOOT){//после перезагрузки надо проверить новый адрес сервера
                    readNextServerAddress()
                }
                else
                    checkHour()
                //startForegroundCompat(R.string.app_name, new Notification());
                val NOTIFICATION_ID = (System.currentTimeMillis() % 10000).toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForeground(NOTIFICATION_ID,
                            NotificationCompat.Builder(this, m_channelId)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    // .setContentTitle("")
                                    // .setContentText("")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .build()
                    )
                //else
                  //  startForegroundCompat(R.string.app_name, Notification())
            }
            if (strAction != null && strAction.equals(ACTION_UPDATE, ignoreCase = true)) { //обновление положения
                advProducer = AdvProducer(this)
               // advProducer!!.readInternalFolder(this)
                UpdateIndicator()
            }
        }
    }

    private fun HideIndicator() {
        if (m_cWM != null) {
            try {
                m_cWM!!.removeView(frameView)
            } catch (ex: Exception) {
            }
        }
        m_cWM = null

    }

    private fun resize(view: View?, scaleX: Float, scaleY: Float) {
        val layoutParams = view!!.layoutParams
        layoutParams.width = (view.width * scaleX).toInt()
        layoutParams.height = (view.height * scaleY).toInt()
        view.layoutParams = layoutParams
    }

    private fun prepareViews(): WindowManager.LayoutParams?{
        try{
            unregisterReceiver(hourReceiver)
        }
        catch (ex: Exception){}
        registerReceiver(hourReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        try {
            var nWidth = WindowManager.LayoutParams.MATCH_PARENT
            var nHeight = WindowManager.LayoutParams.WRAP_CONTENT
            showNextAdvData()
            var nFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            var lp: WindowManager.LayoutParams? = null
            frameView?.layoutParams?.let { lp = it as WindowManager.LayoutParams }
            if (lp == null){
                lp = WindowManager.LayoutParams(nWidth, nHeight,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        nFlags, PixelFormat.TRANSLUCENT)
            }
            else{
                lp?.flags=nFlags
            }
            lp?.gravity=Gravity.BOTTOM //or Gravity.FILL_HORIZONTAL
            lp?.width = nWidth
            lp?.height = nHeight
            return lp
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun UpdateIndicator(){
        try {
            m_cWM = getSystemService(WINDOW_SERVICE) as WindowManager
            m_cWM!!.updateViewLayout(frameView, prepareViews())
            //resize(m_cIVLogo, fScale, fScale);
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        checkHour()
    }

    private fun showIndicator() {
        prevHour=-1
        try {
            val cInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            frameView = cInflater.inflate(R.layout.onscreen_view, null)
            val textView = frameView?.findViewById(R.id.textViewRunText) as RunningTextView// TextView?
            textView?.let {
                val cPrefs = getSharedPreferences("Common", MODE_PRIVATE)
                it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight,
                        cPrefs?.getInt("bottom margin", 0)!!)
            }
            m_cWM = getSystemService(WINDOW_SERVICE) as WindowManager
            val overlayView = prepareViews()
            overlayView?.let { m_cWM!!.addView(frameView, it) }
            //resize(frameView, fScale, fScale)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        checkHour()
    }

    fun showNextAdvDataAsync(pause: Int){//запуск смены рекламы
        handler.removeCallbacks(nextAdvDataRunnable)
        if (frameView!=null){
            frameView?.post({
                frameView?.visibility=View.GONE
                handler.postDelayed(nextAdvDataRunnable, 1000L*pause)
            })
        }
        else {
            handler.postDelayed(nextAdvDataRunnable, 1000L * pause)
        }
        //handler.post(nextAdvDataRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun showNextAdvData(){
        handler.removeCallbacks(nextAdvDataRunnable)
        val imageView = frameView?.findViewById(R.id.imageViewPIC) as ImageView?
        val textView = frameView?.findViewById(R.id.textViewRunText) as RunningTextView// TextView?
        val advData = advProducer?.next()
        if (advData == null){//нет рекламы - выключаем ее
            frameView?.visibility=View.GONE
            return
        }
        frameView?.visibility=View.VISIBLE
        advData?.let {
            try {
                if (imageView != null&&it.imagePath!=null) {
                    if (it.isGif){
                        Glide
                                .with(this)
                                .asGif()
                                .load(it.imagePath)
                                //.override(256, 256)
                                //.fitCenter()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageView)
                                .getSize({ width: Int, height: Int ->
                                    adjustSizeImageView(imageView, width, height, it)
                                })
                    }
                    else {
                        Glide
                                .with(this)
                                .asBitmap()
                                .load(it.imagePath)
                                //.override(256, 256)
                                //.fitCenter()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageView)
                                .getSize({ width: Int, height: Int ->
                                    adjustSizeImageView(imageView, width, height, it)
                                })
                    }
                }
            } finally {
            }
            textView?.visibility = View.VISIBLE
            val myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, it.textSize, getResources().getDisplayMetrics())
            textView?.textSize = myTextSize
            if (it.text!=null)
                textView?.text= it.text!!
            textView?.textColor=it.textColor
            textView?.textBackground=it.textBackground
            textView?.heightRect=it.height
            val marginBottom = it.marginBottom//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, it.marginBottom.toFloat(), getResources().getDisplayMetrics())
            textView?.marginBottom= marginBottom.toInt()
            //textView?.isSelected = true
            showCount=it.showCount
            textView?.endCycleCallback = {//колбэк когда закончился текущий цикл показа строки
                if (showCount>0) {
                    showCount--
                    if (showCount <= 0) {//меняем на следующий показ
                        showNextAdvDataAsync(it.pause)
                    }
                }
            }
            //handler.postDelayed(nextAdvDataRunnable, it.duration * 1000L)
        }
    }

    fun readNextDataFromServer(address: String){//чтение данных с сервера
        advProducer?.readNextDataFromServer(this, address, {//колбэк для результата
            showNextAdvDataAsync(0)
        }, {//колбэк для ошибки - смена сервера
            readNextServerAddress()
        })
    }

    private fun readNextServerAddress(){
        val currentDateTime = Calendar.getInstance()
        val curHour=currentDateTime.get(Calendar.HOUR_OF_DAY)
        if (curHour!=prevHourAddress) {//час изменился, читаем новые данные
            prevHourAddress = curHour
            advProducer?.readNextServerAddress(this, "https://www.transportcompany.top/reklama/address.txt") {
                if (it == null) {//адрес сервера не прочитан, работаем по старинке
                    checkHour()
                } else {//читаем новый адрес и новые данные с него
                    //DebugLog.e(TAG, "New source found in master = " + it!!)
                    val cPrefs = getSharedPreferences("Common",
                            AppCompatActivity.MODE_PRIVATE).edit()
                    cPrefs?.putString("server", it!!)
                    cPrefs.commit()
                    makeDescriptionAddr()?.let {
                        readNextDataFromServer(it)
                    }
                }
            }
        }
    }

    private fun makeDescriptionAddr():String?{
        val currentDateTime = Calendar.getInstance()
        val format = "MM/dd/HH" //для полной версии
        //val format = "HH" //для отладки
        val sdf = SimpleDateFormat(format)
        val currentDate = sdf.format(currentDateTime.time)
        val cPrefs = getSharedPreferences("Common", MODE_PRIVATE)
        var newAddress = cPrefs?.getString("server", "https://miner.net.ru/reklama")
        if (newAddress!=null&&newAddress.length>0){
            if (newAddress.length>0&&newAddress[newAddress.length - 1]!='/')
                newAddress+="/"
            newAddress+=currentDate+"/description.txt"
        }
      //  DebugLog.e(TAG, "new list = " + newAddress)
        return newAddress
    }

    private fun checkHour(){
        val currentDateTime = Calendar.getInstance()
        val curHour=currentDateTime.get(Calendar.HOUR_OF_DAY)
        if (curHour!=prevHour){//час изменился, читаем новые данные
            //val format = "yyyyMMdd/HH"
            prevHour=curHour
            makeDescriptionAddr()?.let {
                readNextDataFromServer(it)
                sendMACAddress()
            }
        }
    }

    inner class TimeBroadcatReceiver: BroadcastReceiver() {//ресивер, отрабатывающий каждую минуту
        override fun onReceive(context: Context?, intent: Intent?) {
            checkHour()
        }
    }

    private fun adjustSizeImageView(imageView: ImageView, width: Int, height: Int, advData: AdvData){//подбор размера ImageView для вписывания картинки
        if (width == 0 || height == 0) {
            return
        }
        var maxSize=advData.height
        var newWidth=0
        var newHeight=0
        /*if (width>=height){//ширина будет основополагающим параметром для подбора
            newWidth=minOf(width, maxSize)
            newHeight = height*newWidth/width
        }
        else{//иначе - высота*/
            val myShift = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, maxSize.toFloat(), getResources().getDisplayMetrics())
            newHeight=minOf(height, myShift.toInt())
            newWidth = newHeight*width/height
        //}
        imageView.requestLayout()
        imageView.layoutParams.height=newHeight
        imageView.layoutParams.width=newWidth
        imageView.requestLayout()
    }

    private fun sendMACAddress(){
        handler.removeCallbacks(sendMACAddressRunnable)
        advProducer?.sendMACAddress()
        handler.postDelayed(sendMACAddressRunnable, 1000*3600)
    }
}
