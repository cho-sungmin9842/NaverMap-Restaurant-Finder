package com.example.navermap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.navermap.databinding.ItemRestarantBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.Tm128

class RestarantListAdapter(private val onClick: (LatLng) -> Unit) :
    RecyclerView.Adapter<RestarantListAdapter.ViewHoler>() {
    private var dataSet = emptyList<SearchItem>()

    fun setDataSet(dataSet: List<SearchItem>) {
        // 인자로 받은 데이터(List<SearchItem>)를 저장함
        this.dataSet = dataSet
        // adapter의 데이터가 바뀌었다고 알림
        notifyDataSetChanged()
    }

    class ViewHoler(private val binding: ItemRestarantBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SearchItem, onClick: (LatLng) -> Unit) {
            // 식당이름을 보여주는 텍스트뷰 설정
            binding.titleTextView.text = item.plainTitle
            // 식당의 음식에 카테고리나 종류(일식,중식,양식,한식 등)를 보여주는 텍스트 뷰 설정
            binding.categoryTextView.text = item.category
            // 식당의 위치를 보여주는 텍스트뷰 설정
            binding.locationTextView.text = item.roadAddress
            // 해당 식당을 클릭한 경우
            binding.root.setOnClickListener {
                // 선택한 항목의 음식점으로 이동
//                onClick(Tm128(item.mapx.toDouble(), item.mapy.toDouble()).toLatLng())
                onClick(LatLng(item.mapy * 0.0000001, item.mapx * 0.0000001))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHoler {
        return ViewHoler(
            ItemRestarantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHoler, position: Int) {
        holder.bind(dataSet[position], onClick)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}