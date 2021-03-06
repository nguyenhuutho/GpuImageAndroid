package android.sticky.com.ui.text

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.sticky.com.R
import android.sticky.com.lib.Config
import android.sticky.com.lib.StickerGroup
import android.sticky.com.lib.event.BottomRightIconEvent
import android.sticky.com.lib.event.TopLeftIconEvent
import android.sticky.com.lib.event.flip.FlipHorizontallyEvent
import android.sticky.com.lib.event.parent.OnStickerOperationListener
import android.sticky.com.lib.stickeritem.DrawableSticker
import android.sticky.com.lib.stickeritem.IconMenuOption
import android.sticky.com.lib.stickeritem.Sticker
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.*
import com.sample.huutho.utils.*
import java.io.File
import java.util.*

class TextStickyFragment : Fragment(), OnStickerOperationListener, KeyboardHeightObserver {

    companion object {
        const val MODE_KEYBOARD = 0x11
        const val MODE_EDIT = 0x12

        fun newInstance() = TextStickyFragment()
    }

    /** The keyboard height provider  */
    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

    private lateinit var mTitle: TextView
    private lateinit var mImageView: ImageView
    private lateinit var mStickerGroup: StickerGroup
    private lateinit var mLayoutEdittext: RelativeLayout
    private lateinit var mStickerGroupContainer: RelativeLayout

    private lateinit var mLayoutOption: RelativeLayout
    private lateinit var mImageDone: ImageView
    private lateinit var mImagePlus: ImageView
    private lateinit var mEditInput: EditText

    private lateinit var mContainerFragment: RelativeLayout

    private var mIsActive = true

    private var mKeyboardHeight = 10


    /**********************************************************************************************/
    private val start = {}

    private val success = { bmp: Bitmap ->
        mImageView.setImageBitmap(bmp)
        mStickerGroup.layout(mImageView.left, mImageView.top, mImageView.right, mImageView.bottom)
        mStickerGroup.invalidate()
    }

    private val failure = { Toast.makeText(context, "failure", Toast.LENGTH_SHORT).show() }
    /**********************************************************************************************/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyboardHeightProvider = KeyboardHeightProvider(activity);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_text_sticky, container, false)

        view.post { keyboardHeightProvider.start() }

        mTitle = view.findViewById(R.id.control_bar_title)
        mImageView = view.findViewById(R.id.image_view)
        mStickerGroup = view.findViewById(R.id.sticker_group)
        mLayoutEdittext = view.findViewById(R.id.relative_layout_edittext)

        mStickerGroupContainer = view.findViewById(R.id.relative_layout_sticker_group)

        mLayoutOption = view.findViewById(R.id.relative_layout_option)

        mContainerFragment = view.findViewById(R.id.relative_layout_container_fragment)

        mEditInput = view.findViewById(R.id.edit_input)
        mImagePlus = view.findViewById(R.id.image_clear)
        mImageDone = view.findViewById(R.id.image_done)

        mImagePlus.setOnClickListener { }
        mImageDone.setOnClickListener { }

        view.findViewById<ImageView>(R.id.control_bar_cancel).setOnClickListener { onCancel() }
        view.findViewById<ImageView>(R.id.control_bar_done).setOnClickListener { onSave() }

        context?.getBitmapFromTempCache(start, success, failure)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initStickerGroup()
    }

    private fun onSave() {
        val saveSuccess = { _: File -> activity?.supportFragmentManager?.popBackStackImmediate().run {} }

        val saveFailure = {
            activity?.supportFragmentManager?.popBackStackImmediate()
            Toast.makeText(context, "Save failur", Toast.LENGTH_SHORT).show()
        }

        context?.saveBitmapToTempCache(mStickerGroup.createBitmap(), saveSuccess, saveFailure)
    }

    override fun onResume() {
        super.onResume()
        keyboardHeightProvider.setKeyboardHeightObserver(this)
    }

    override fun onPause() {
        super.onPause()
        keyboardHeightProvider.setKeyboardHeightObserver(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardHeightProvider.close()
    }


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        val or = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape";
        Log.i("TextStickyFragment", "onKeyboardHeightChanged in pixels: $height $or")

        // Điểm neo : chính là điểm góc trên bên trái của bàn phím
        if (mIsActive) {
            val pointAnchor = getScreenHeight() - height
            mLayoutEdittext.animate()
                    .y((pointAnchor - mLayoutEdittext.height).toFloat())
                    .setDuration(130)
                    .setInterpolator(AccelerateInterpolator())
                    .start()
        }

    }

    private fun onCancel() {
        Toast.makeText(context, "onCancel", Toast.LENGTH_SHORT).show()
        activity?.supportFragmentManager?.popBackStack()
    }


    /************************************** INNER FUNCTION ****************************************/

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun showFragment(fragment: Fragment) {

        context?.forceHideKeyBoard()

        val anchorYLayoutEdit = getScreenHeight() - mKeyboardHeight - mLayoutOption.height - mLayoutEdittext.height
        if (mLayoutEdittext.y != anchorYLayoutEdit.toFloat())
            mLayoutEdittext
                    .animate()
                    .y(anchorYLayoutEdit.toFloat())
                    .setInterpolator(AccelerateInterpolator())
                    .start()


        val anchorYLayoutOption = mLayoutEdittext.y + mLayoutEdittext.height
        mLayoutOption.visible()
        if (mLayoutOption.y != anchorYLayoutOption)
            mLayoutOption
                    .animate()
                    .y(mLayoutEdittext.y + mLayoutEdittext.height)
                    .setInterpolator(AccelerateInterpolator())
                    .start()


        val childFrag = childFragmentManager.findFragmentById(R.id.relative_layout_container_fragment)
        if (childFrag != fragment ){
            childFragmentManager.beginTransaction()
                    .add(R.id.relative_layout_container_fragment, fragment)
                    .commit()
        }

        Toast.makeText(context, "Hide keyboard - add Fragment to Container Fragment", Toast.LENGTH_SHORT).show()
    }

    private fun showKeyBoard() {

    }


    private fun initStickerGroup() {

        context?.let {
            val deleteIcon = IconMenuOption(ContextCompat.getDrawable(it, R.drawable.sticker_ic_close_white_18dp), Config.Gravity.LEFT_TOP)
            deleteIcon.iconEventListener = TopLeftIconEvent()

            val zoomIcon = IconMenuOption(ContextCompat.getDrawable(it, R.drawable.sticker_ic_scale_white_18dp), Config.Gravity.RIGHT_BOTOM)
            zoomIcon.iconEventListener = BottomRightIconEvent()

            val flipIcon = IconMenuOption(ContextCompat.getDrawable(it, R.drawable.sticker_ic_flip_white_18dp), Config.Gravity.RIGHT_TOP)
            flipIcon.iconEventListener = FlipHorizontallyEvent()


            mStickerGroup.optionIcons = Arrays.asList(deleteIcon, zoomIcon, flipIcon)
            mStickerGroup.setBackgroundColor(Color.WHITE)
            mStickerGroup.isLocked = false
            mStickerGroup.isConstrained = true
            mStickerGroup.onStickerOperationListener = this
        }
    }

    /**
     * Lock selected sticker
     * Hide all option menu
     */
    fun lockSticker() {
        mStickerGroup.isLocked = !mStickerGroup.isLocked
    }


    /**
     * Remove current selected sticker in ViewGroup
     */
    fun removeCurrentSticker() {
        if (mStickerGroup.removeCurrentSticker()) {
            Toast.makeText(this.context!!, "Remove current Sticker successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.context!!, "Remove current Sticker failed!", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Remove all sticker in ViewGroup
     */
    private fun removeAllSticker() {
        mStickerGroup.removeAllStickers()
    }


    /**
     * Add Sticker to ViewGroup
     * @param pathAsset path of image_sticker in assets folder
     */
    private fun addSticker(pathAsset: String) {
        val d = Drawable.createFromStream(this.context!!.assets.open(pathAsset), null)
        mStickerGroup.addSticker(DrawableSticker(d))


    }


    /**************************************** Sticker Callback ************************************/

    override fun onStickerAdded(sticker: Sticker) {
    }

    override fun onStickerClicked(sticker: Sticker) {
    }

    override fun onStickerDeleted(sticker: Sticker) {
    }

    override fun onStickerDragFinished(sticker: Sticker) {
    }

    override fun onStickerTouchedDown(sticker: Sticker) {
    }

    override fun onStickerZoomFinished(sticker: Sticker) {
    }

    override fun onStickerFlipped(sticker: Sticker) {
    }

    override fun onStickerDoubleTapped(sticker: Sticker) {
    }
}