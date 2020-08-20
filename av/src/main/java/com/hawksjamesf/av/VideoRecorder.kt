package com.hawksjamesf.av

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.view.*
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.Utils
import java.io.File
import java.io.FileDescriptor

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Aug/18/2020  Tue
 */
class VideoRecorder constructor(val context: Context)
    : SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener,
        TextureView.SurfaceTextureListener {
    companion object {
        private const val TAG = "Chaplin/VideoRecorder"
        fun createAndBindScreen(context: Context, mediaProjection: MediaProjection, outputFile: File): VideoRecorder {
            val screenRecorder = VideoRecorder(context)
            screenRecorder.setOutput(outputFile)
            screenRecorder.bindScreen(mediaProjection)
            return screenRecorder
        }
        fun createAndBindScreen(context: Context, mediaProjection: MediaProjection, outputfd: FileDescriptor): VideoRecorder {
            val screenRecorder = VideoRecorder(context)
            screenRecorder.setOutput(outputfd)
            screenRecorder.bindScreen(mediaProjection)
            return screenRecorder
        }

        fun createAndBindCameraFacingBack(context: Context,
                                          surfaceView: SurfaceView) =
                createAndBind(context,
                        surfaceView = surfaceView,
                        cameraId = Camera.CameraInfo.CAMERA_FACING_BACK)

        fun createAndBindCameraFacingBack(context: Context,
                                          textureView: TextureView) =
                createAndBind(context,
                        textureView = textureView,
                        cameraId = Camera.CameraInfo.CAMERA_FACING_BACK)

        fun createAndBindCameraFacingFront(context: Context,
                                           surfaceView: SurfaceView) =
                createAndBind(context,
                        surfaceView = surfaceView,
                        cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT)

        fun createAndBindCameraFacingFront(context: Context,
                                           textureView: TextureView) =
                createAndBind(context,
                        textureView = textureView,
                        cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT)

        private fun createAndBind(context: Context, surfaceView: SurfaceView? = null, textureView: TextureView? = null, cameraId: Int): VideoRecorder {
            val recorder = VideoRecorder(context)
            if (textureView != null) {
                recorder.bindTextureView(textureView, cameraId)
            } else if (surfaceView != null) {
                recorder.bindSurfaceView(surfaceView, cameraId)
            }
            return recorder
        }
    }

    private val mMediaRecorder: MediaRecorder = MediaRecorder()
    private var mCameraId: Int = -1
    private val mCamera: Camera? by lazy {
        return@lazy try {
            val c = Camera.open(mCameraId)
            c.setDisplayOrientation(90) // attempt to get a Camera instance
            c
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }
    private lateinit var mSurfaceView: SurfaceView
    private var mSurfaceHolder: SurfaceHolder? = null
    private lateinit var mTextureView: TextureView
    private var mSurfaceTexture: SurfaceTexture? = null
    private lateinit var mSupportedVideoSizes: List<Camera.Size>
    private lateinit var mSupportedPreviewSizes: List<Camera.Size>
    private var mOutputFile: File? = null
    private var mOutputfd: FileDescriptor? = null
    private lateinit var mMediaProjection: MediaProjection

    init {
    }

    private fun bindScreen(mediaProjection: MediaProjection) {
        mMediaProjection = mediaProjection
        config()
        val width = getScreenWidth()
        val height = getScreenHeight()
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        profile.videoFrameWidth = width
        profile.videoFrameHeight = height
        mMediaRecorder.setProfile(profile)
        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile!!.absolutePath)
        } else if (mOutputfd != null) {
            mMediaRecorder.setOutputFile(mOutputfd)
        }
        //                setVideoFrameRate()
        mMediaRecorder.prepare()
        val virtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", width, height, getScreenDensityDpi(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.surface, null, null)

    }

    private fun bindSurfaceView(surfaceView: SurfaceView, cameraId: Int) {
        mCameraId = cameraId
        mSurfaceView = surfaceView
        mSurfaceView.holder.addCallback(this)
        if (mCameraId != -1) {
            val parameters = mCamera?.parameters
            mSupportedVideoSizes = parameters!!.supportedVideoSizes
            mSupportedPreviewSizes = parameters!!.supportedPreviewSizes
            mMediaRecorder.setCamera(mCamera)
        }
    }

    private fun bindTextureView(textureView: TextureView, cameraId: Int) {
        mCameraId = cameraId
        mTextureView = textureView
        mTextureView.surfaceTextureListener = this
        if (mCameraId != -1) {
            val parameters = mCamera?.parameters
            mSupportedVideoSizes = parameters!!.supportedVideoSizes
            mSupportedPreviewSizes = parameters!!.supportedPreviewSizes
            mMediaRecorder.setCamera(mCamera)
        }

    }

    private fun config() {
        if (mCameraId != -1) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        } else {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        }

    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated");
        mSurfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        mSurfaceHolder = null
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged format:" + format + "--->surface size:" + width + "/" + height)
        mSurfaceHolder = holder
        if (mCameraId != -1) {
            holder?.let { mCamera?.restartPreview(it) }
        }
        config()
        val optimalVideoSize = getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, width, height)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        profile.videoFrameWidth = optimalVideoSize?.width ?: 0
        profile.videoFrameHeight = optimalVideoSize?.height ?: 0
//        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        mCamera?.setParameters(parameters);

        mMediaRecorder.setProfile(profile)
        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile!!.absolutePath)
        } else if (mOutputfd != null) {
            mMediaRecorder.setOutputFile(mOutputfd)
        }
//            mMediaRecorder.setVideoSize(width, height)
        //                setVideoFrameRate()
//        mMediaRecorder.setPreviewDisplay(Surface(mSurfaceTexture))
        mMediaRecorder.prepare()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        Log.d(TAG, "onSurfaceTextureUpdated");
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        return false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        mSurfaceTexture = surface
        if (mCameraId != -1) {
            surface?.let { mCamera?.restartPreview(it) }
        }
        config()
        val optimalVideoSize = getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, width, height)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
//        profile.videoFrameWidth = optimalVideoSize?.width ?: 0
//        profile.videoFrameHeight = optimalVideoSize?.height ?: 0
//        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        mCamera?.setParameters(parameters);

        mMediaRecorder.setProfile(profile)
        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile!!.absolutePath)
        } else if (mOutputfd != null) {
            mMediaRecorder.setOutputFile(mOutputfd)
        }
//            mMediaRecorder.setVideoSize(width, height)
        //                setVideoFrameRate()
//        mMediaRecorder.setPreviewDisplay(Surface(mSurfaceTexture))
        mMediaRecorder.prepare()
    }

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(VideoPlayer.TAG, "onError：what/desc:$what--->extra/desc:$extra")
    }

    override fun onInfo(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(VideoPlayer.TAG, "onInfo：what/desc:$what--->extra:$extra")

    }


    @AnyThread
    fun start() {
        if (mCameraId != -1) mCamera?.unlock()
        mMediaRecorder.start()

    }

    @RequiresApi(Build.VERSION_CODES.N)
    @AnyThread
    fun pause() {
        mMediaRecorder.pause()
    }

    @AnyThread
    fun stop() {
        mMediaRecorder.stop()
        mMediaRecorder.release()
        if (mCameraId != -1) {
            mCamera?.lock()
            mCamera?.release()
        }
    }


    fun setOutput(file: File) = apply {
        mOutputFile = file
    }

    fun setOutput(fd: FileDescriptor) = apply {
        mOutputfd = fd
    }

    private fun getScreenWidth(): Int {
        val wm = Utils.getApp().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ?: return -1
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.defaultDisplay.getRealSize(point)
        } else {
            wm.defaultDisplay.getSize(point)
        }
        return point.x
    }

    private fun getScreenHeight(): Int {
        val wm = Utils.getApp().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ?: return -1
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.defaultDisplay.getRealSize(point)
        } else {
            wm.defaultDisplay.getSize(point)
        }
        return point.y
    }

    private fun getScreenDensityDpi(): Int {
        return Resources.getSystem().displayMetrics.densityDpi
    }


    fun getOptimalVideoSize(supportedVideoSizes: List<Camera.Size>?,
                            previewSizes: List<Camera.Size>, w: Int, h: Int): Camera.Size? {
        // Use a very small tolerance because we want an exact match.
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        val videoSizes: List<Camera.Size>
        videoSizes = supportedVideoSizes ?: previewSizes
        var optimalSize: Camera.Size? = null

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        var minDiff = Double.MAX_VALUE

        // Target view height

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (size in videoSizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff && previewSizes.contains(size)) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in videoSizes) {
                if (Math.abs(size.height - h) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }


}