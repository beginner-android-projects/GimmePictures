package ua.andrii.andrushchenko.gimmepictures.ui.base

import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BasePagedAdapter<Entity : Any>(diffUtil: DiffUtil.ItemCallback<Entity>) :
    PagingDataAdapter<Entity, BasePagedAdapter<Entity>.BaseViewHolder>(diffUtil) {

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) holder.bind(currentItem)
    }

    abstract inner class BaseViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(entity: Entity)
    }
}