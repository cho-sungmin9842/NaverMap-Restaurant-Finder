package com.example.navermap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navermap.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private lateinit var lifecycleState: String

    // NaverMap이 초기화 되었는지 여부를 가진 변수(맨 처음에는 초기화되지 않았으므로 false로 설정)
    private var isMapInit = false

    // 맛집목록을 보여주는 RecyclerView의 Adapter 생성 및 설정
    private val restarantListAdapter = RestarantListAdapter {
        // BottomSheetBehavior 설정
        collapseBottomSheet()
        // recyclerView에서 클릭한 맛집으로 카메라와 마커를 이동함
        moveCamera(it, 17.0)
    }

    // 지도에 그릴 마커 리스트
    private var markers = emptyList<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // NaverMap 설정
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        lifecycleState = lifecycle.currentState.toString()
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        // 맛집목록을 보여주는 RecyclerView 생성 및 설정(layoutManager,adapter)
        binding.bottomSheetLayout.searchResultRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = restarantListAdapter
        }
        // 검색,변경 이벤트 관리 리스너
        binding.searchView.setOnQueryTextListener(object : OnQueryTextListener {
            // 검색버튼을 눌렀을 경우
            override fun onQueryTextSubmit(query: String?): Boolean {
                return if (query?.isNotEmpty() == true) {   // 입력한 문자열이 빈문자열이 아니라면
                    SearchRepository.getGoodRestaurant(query)
                        .enqueue(object : Callback<SearchResult> {
                            // 응답 있을 시(성공시)
                            override fun onResponse(
                                call: Call<SearchResult>,
                                response: Response<SearchResult>
                            ) {
                                val searchItemList = response.body()?.items.orEmpty()
                                // 응답받은 데이터가 빈 경우
                                if (searchItemList.isEmpty()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "검색 결과가 없습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return
                                } else if (isMapInit.not()) { // 지도가 초기화 되지 않은 경우
                                    Toast.makeText(
                                        this@MainActivity,
                                        "오류가 발생했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return
                                }
                                // markers 리스트에 지도를 null로 초기화함
                                markers.forEach {
                                    it.map = null
                                }
                                // markers 리스트에 생성한 Marker 객체 담기
                                markers = searchItemList.map {
                                    // Marker 객체 생성 및 설정(타이틀,지도를 NaverMap 객체,위경도 값)
                                    Marker(
                                        LatLng(
                                            it.mapy * 0.0000001,
                                            it.mapx * 0.0000001,
                                        )
                                    ).apply {
                                        captionText = it.plainTitle
                                        map = naverMap
                                    }
                                }
                                markers.forEach { marker ->
                                    marker.setOnClickListener {
                                        moveCamera(marker.position, 17.0)
                                        true
                                    }
                                }
                                // 응답받은 List<SearchItem> 데이터를 adapter에 보냄
                                restarantListAdapter.setDataSet(searchItemList)
                                // BottomSheetBehavior 설정
                                collapseBottomSheet()
                                // 응답으로 받은 첫번째 항목으로 카메라와 마커를 이동함
                                moveCamera(markers.first().position, 13.0)
                            }

                            // 응답 없을 시(실패시)
                            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                                t.printStackTrace()
                                println("에러=${t.message}")
                            }
                        })
                    false // 기본 액션(기본값)
                } else {    // 입력한 문자가 빈문자열이라면
                    true // true는 사용이 다 되지 않음
                }
            }

            // 입력한 텍스트가 바뀔때마다 호출
            override fun onQueryTextChange(newText: String?): Boolean {
                return true //
            }
        })
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (lifecycleState == "CREATED") {
            //위치 권한이 이미 주어진 경우
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            //위치 권한이 주어지지 않은 경우
            else {
                showDialog()
            }
        }
        binding.mapView.onResume()

    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        lifecycleState = lifecycle.currentState.toString()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    // 지도가 준비가 되면 호출됨
    override fun onMapReady(mapObject: NaverMap) {
        // 인자로 온 NaverMap 객체를 받아옴
        naverMap = mapObject
        // 지도가 준비되어 초기화 되었으므로 true로 설정
        isMapInit = true
        naverMap.uiSettings.isLocationButtonEnabled = false
        naverMap.uiSettings.isCompassEnabled = false
        naverMap.uiSettings.isZoomControlEnabled = true
        naverMap.locationSource = locationSource
        binding.location.map = naverMap
        binding.compass.map = naverMap
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // camera를 움직이는 함수
    private fun moveCamera(position: LatLng, zoomLevel: Double) {
        // NaverMap이 초기화 되지 않았다면 리턴함
        if (isMapInit.not()) return
        // CameraUpdate 객체 생성 및 설정
        // animation을 CameraAnimation.Easing=부드럽게 가감속하며 이동, 가까운 거리를 이동할 때 적합
        // 확대할 정도를 갖는 ZoomLevel 값 만큼 확대
        // 위경도 정보를 갖고 있는 LatLng 객체(position)
        val cameraUpdate = CameraUpdate.scrollAndZoomTo(position, zoomLevel)
            .animate(CameraAnimation.Easing)
        // naverMap의 카메라를 cameraUpdate로 이동한다.
        naverMap.moveCamera(cameraUpdate)
    }

    // BottomSheetBehavior 설정하는 함수
    private fun collapseBottomSheet() {
        // layout_behavior를 BottomSheetBehavior로 설정
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout.root)
        // STATE_COLLAPSED : 접혀있는 상태
        bottomSheetBehavior.state = STATE_COLLAPSED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            } else {
                naverMap.locationTrackingMode = LocationTrackingMode.Follow
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("위치 정보를 얻기위해서는 위치 권한이 필요합니다")
            setPositiveButton("허용") { _, _ ->
                val findLocation = ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                )
                val courseLocation = ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (findLocation && courseLocation) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ), LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                    finish()
                }
            }
            setNegativeButton("취소", null)
            show()
        }
    }
}