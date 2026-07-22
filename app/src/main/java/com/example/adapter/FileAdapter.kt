package com.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.model.FileItem

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onSelectionToggle: (FileItem) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    var selectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectionMode, onItemClick, onItemLongClick, onSelectionToggle)
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtFileName)
        private val txtDetails: TextView = itemView.findViewById(R.id.txtFileDetails)
        private val imgIcon: ImageView = itemView.findViewById(R.id.imgFileIcon)
        private val chkSelect: CheckBox = itemView.findViewById(R.id.chkSelect)

        fun bind(
            item: FileItem,
            selectionMode: Boolean,
            onItemClick: (FileItem) -> Unit,
            onItemLongClick: (FileItem) -> Unit,
            onSelectionToggle: (FileItem) -> Unit
        ) {
            txtName.text = item.name
            txtDetails.text = if (item.isDirectory) "${item.itemCount ?: 0} items | ${item.formattedDate}" else "${item.formattedSize} | ${item.formattedDate}"

            if (selectionMode) {
                chkSelect.visibility = View.VISIBLE
                chkSelect.isChecked = item.isSelected
                chkSelect.setOnClickListener { onSelectionToggle(item) }
            } else {
                chkSelect.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
