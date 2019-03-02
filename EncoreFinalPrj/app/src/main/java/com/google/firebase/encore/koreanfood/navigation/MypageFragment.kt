package com.google.firebase.encore.koreanfood.navigation

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.encore.koreanfood.R
import com.google.firebase.encore.koreanfood.dataModel.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.android.synthetic.main.shimmer_layout.*


class MypageFragment : Fragment() {

    var user: FirebaseUser? = null
    var firestore: FirebaseFirestore? = null
    var firebaseStorage: FirebaseStorage? = null

    var mainView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // mainView = inflater.inflate(R.layout.fragment_mypage, container, false)

        user = FirebaseAuth.getInstance().currentUser
        mainView = inflater.inflate(R.layout.shimmer_layout, container, false)
        firestore = FirebaseFirestore.getInstance()

        return mainView
    }

    override fun onResume() {
        super.onResume()

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = RecyclerAdapter()

        val swipeHandler = object : SwipeToDeleteCallback(context!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                val adapter = recyclerView.adapter as RecyclerAdapter
                adapter.removeAt(viewHolder!!.adapterPosition)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    inner class RecyclerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val contentDTOs_adapter: MutableList<ContentDTO>

        init {
            contentDTOs_adapter = ArrayList()
            setValue()
            firebaseStorage = FirebaseStorage.getInstance()

        }
         // FireStore로부터 값 불러오는 메서드
        fun setValue() {
            firestore?.collection("photos")?.orderBy("timestamp", Query.Direction.DESCENDING)?.addSnapshotListener {
                    querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot != null){
                        for (dc in querySnapshot!!.documents) {
                            var contentDTO = dc.toObject(ContentDTO:: class.java)

                            if (contentDTOs_adapter.contains(contentDTO) == false) {
                                contentDTOs_adapter?.add(contentDTO!!)
                            }

                            Log.d("contentDTOs_set", contentDTOs_adapter.toString())
                            notifyDataSetChanged()
                        }
                    }
            }
        }

        fun removeAt(position:Int) {

            var temp = contentDTOs_adapter[position]

            // 1. 화면상에서 제거
            contentDTOs_adapter.removeAt(position)
            notifyDataSetChanged()

            // 2. DB, 저장소에서 실제로 값 제거
            val deleteURl = temp.photoUrl
            val gsReference = firebaseStorage?.getReferenceFromUrl(deleteURl!!)
            val documentName = "JPEG_" + temp.timestamp + "_.png"
            // Storage에서 사진 제거
            gsReference?.delete()?.addOnSuccessListener {
                // 사진 제거 성공시 DB에서 값 제거
                firestore?.collection("photos")?.document(documentName)?.delete()?.addOnCompleteListener{
//                    notifyDataSetChanged()
//                    notifyItemRemoved(position)
                }
            }
            Log.d("DTO제거시점", contentDTOs_adapter.size.toString())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            // Firebase Storage, CloudFireStore로부터 사진, 후기 정보 받아오는 부분
            val viewHolder = (holder as CustomViewHolder).itemView

            Log.d("contentDTOs_on", contentDTOs_adapter.toString())

            val gsReference = firebaseStorage?.getReferenceFromUrl(contentDTOs_adapter!![position]?.photoUrl!!)
            // Log.d("참조", gsReference?.downloadUrl.toString())
            gsReference?.downloadUrl?.addOnSuccessListener {
                // it -> FireBaseStorage에 저장된 사진의 downloadUrl
                Glide.with(holder.itemView.context).load(it).into(viewHolder.photoView)
                // timestamp (찍은 시간)
                viewHolder.time.text = contentDTOs_adapter!![position]?.timestamp
                // userid(아이디)
                // viewHolder.email.text = contentDTOs_adapter!![position]?.userId
                // 음식 이름
                viewHolder.title.text = contentDTOs_adapter!![position]?.foodname
                // 리뷰
                viewHolder.content.text = contentDTOs_adapter!![position]?.review
            }

        }
        override fun getItemCount(): Int {
            Log.d("사이즈는", contentDTOs_adapter!!.size.toString())
            return contentDTOs_adapter!!.size
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    }

    companion object {
        fun newInstance(): MypageFragment = MypageFragment()
    }
}

abstract class SwipeToDeleteCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white_24)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth
    private val intrinsicHeight = deleteIcon?.intrinsicHeight
    private val background = ColorDrawable()
    private val backgroundColor = Color.parseColor("#f44336")
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }


    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {

        if (viewHolder?.adapterPosition == 10) return 0
        return super.getMovementFlags(recyclerView, viewHolder)
    }
    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
        return false
    }
    override fun onChildDraw(
        c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {

        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.color = backgroundColor
        background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        background.draw(c)

        // Calculate position of delete icon

        Log.d("폭높이1", intrinsicHeight.toString())
        Log.d("폭높이2", intrinsicWidth.toString())

        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight!!) / 2
        val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
        val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth!!
        val deleteIconRight = itemView.right - deleteIconMargin
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        // Draw the delete icon
        deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}