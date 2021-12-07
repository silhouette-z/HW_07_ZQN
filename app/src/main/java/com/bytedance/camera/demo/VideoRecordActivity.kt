package com.bytedance.camera.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PictureCallback
import android.media.CamcorderProfile
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_video_record.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VideoRecordActivity : AppCompatActivity() {
    private var mChangeCamera: ImageButton? = null
    private var mWorkButton: ImageButton? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mIsRecording = false
    private var mImageInfo:TextView? = null
    private var mName:EditText? = null
    private var mCameraFacing = CameraInfo.CAMERA_FACING_BACK
    private var mSmallPic:ImageView? = null
    private var mCurrentPhotoPath: String? = null
    private var mVideoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_record)

        mName = findViewById(R.id.name)
        mSurfaceView = findViewById(R.id.surface_view)
        mChangeCamera = findViewById(R.id.change_camera)
        mWorkButton = findViewById(R.id.work_button)
        mImageInfo = findViewById(R.id.image_info)
        mSmallPic = findViewById(R.id.small_image)

        startCamera()
        mChangeCamera?.setOnClickListener(View.OnClickListener { changeCamera() })
        mWorkButton?.setOnClickListener(View.OnClickListener { takePicture()})
        mWorkButton?.setOnLongClickListener(View.OnLongClickListener {recordVideo()})
//        getLastPic()
    }

    private fun changeCamera(){
        if(CameraInfo.CAMERA_FACING_BACK == mCameraFacing) {
            mCameraFacing = CameraInfo.CAMERA_FACING_FRONT
        } else {
            mCameraFacing = CameraInfo.CAMERA_FACING_BACK
        }
        mCamera!!.stopPreview()
        mCamera!!.release()
        mCamera = null

        mCamera = Camera.open(mCameraFacing)
        setCameraDisplayOrientation()
        mCamera!!.setPreviewDisplay(mSurfaceHolder)
        mCamera!!.startPreview()
    }

    private fun startCamera() {
        try {
            mCamera = Camera.open(CameraInfo.CAMERA_FACING_BACK)
            setCameraDisplayOrientation()
        } catch (e: Exception) {
            // error
        }
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    mCamera?.setPreviewDisplay(holder);
                    mCamera?.startPreview();
                } catch (e: IOException) {
                    // error
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
                try {
                    mCamera!!.stopPreview()
                } catch (e: Exception) {
                    // error
                }
                try {
                    mCamera!!.setPreviewDisplay(holder)
                    mCamera!!.startPreview()
                } catch (e: Exception) {
                    //error
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    private fun setCameraDisplayOrientation() {
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val info = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info)
        val result = (info.orientation - degrees + 360) % 360
        mCamera!!.setDisplayOrientation(result)
    }

    private fun takePicture() {
        mCamera!!.takePicture(null, null, PictureCallback { bytes, camera ->
            val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE,mName?.text.toString()) ?: return@PictureCallback
            try {
                val fos = FileOutputStream(pictureFile)
                fos.write(bytes)
                showLastPic(pictureFile)
                fos.close()
            } catch (e: FileNotFoundException) {
                //error
            } catch (e: IOException) {
                //error
            }
            mCamera!!.startPreview()
        })
    }

    private fun getOutputMediaFile(type: Int, name: String): File? {
        // Android/data/com.bytedance.camera.demo/files/Pictures
        val mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!mediaStorageDir!!.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val mediaFile: File
//        val diyName:String = mName?.text.toString()


        mediaFile = if (type == MEDIA_TYPE_IMAGE) {
            if(name != null){File(mediaStorageDir.path + File.separator + "IMG_" + name  + ".jpg")}
                else File(mediaStorageDir.path + File.separator + "IMG_" +name + timeStamp + ".jpg")
        }
            else if (type == MEDIA_TYPE_VIDEO) {
            File(mediaStorageDir.path + File.separator + "VID_"  +timeStamp + ".mp4")
        } else {
            return null
        }
        mCurrentPhotoPath = mediaFile.absolutePath

        return mediaFile
    }

    private fun prepareVideoRecorder(): Boolean {
        mMediaRecorder = MediaRecorder()
        mCamera!!.unlock()
        mMediaRecorder!!.setCamera(mCamera)
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mMediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        mVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO, mName?.text.toString())
        mMediaRecorder!!.setOutputFile(mVideoFile)


        try {
            mMediaRecorder!!.prepare()
        } catch (e: IllegalStateException) {
            releaseMediaRecorder()
            return false
        } catch (e: IOException) {
            releaseMediaRecorder()
            return false
        }
        return true
    }

    private fun recordVideo(): Boolean {
        if (mIsRecording) {
            mMediaRecorder!!.stop()
            releaseMediaRecorder()
            mCamera!!.lock()
            mIsRecording = false
            mVideoFile?.let { showLastPic(it) }

        } else {
            if (prepareVideoRecorder()) {
                mMediaRecorder!!.start()
                mIsRecording = true

            } else {
                releaseMediaRecorder()
            }
        }
        return true
    }

    private fun showLastPic(pictureFile: File) {
        var matrix: Matrix = Matrix()

        var cameraInfo = CameraInfo()
        Camera.getCameraInfo(mCameraFacing, cameraInfo)

        matrix.setRotate(cameraInfo.orientation.toFloat())


        val targetWidth = mSmallPic!!.width
        val targetHeight = mSmallPic!!.height
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var mCurrentPhotoPath = pictureFile.absolutePath
        BitmapFactory.decodeFile(mCurrentPhotoPath, options)
        var photoH = options.outHeight
        var photoW = options.outWidth
        var inSampleSize = 1
        while(photoH > targetHeight || photoW > targetWidth) {
            photoH /= 2
            photoW /= 2
            inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inPurgeable = true
        var bitmap: Bitmap? = null

        if(pictureFile!!.name.startsWith("IMG")) {
            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options)
        } else {
            var mmr = MediaMetadataRetriever()
            mmr.setDataSource(pictureFile.absolutePath)
            bitmap = mmr.getScaledFrameAtTime(-1,MediaMetadataRetriever.OPTION_CLOSEST, targetWidth, targetHeight)

        }

        bitmap = Bitmap.createBitmap(bitmap!!, 0,0,bitmap!!.width, bitmap!!.height, matrix ,true)

        mSmallPic!!.setImageBitmap(bitmap)
        mImageInfo!!.text = "${pictureFile!!.name}" + ""
    }
    private fun showLastInfo(pictureFile: File) {
        mImageInfo!!.text = "$pictureFile!!.name"+"nameaaa"
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaRecorder()
        releaseCamera()
    }

    private fun releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
            mMediaRecorder = null
            mCamera!!.lock()
        }
    }

    private fun releaseCamera() {
        if (mCamera != null) {
            mCamera!!.release()
            mCamera = null
        }
    }

    companion object {
        private const val MEDIA_TYPE_IMAGE = 1
        private const val MEDIA_TYPE_VIDEO = 2
    }
}