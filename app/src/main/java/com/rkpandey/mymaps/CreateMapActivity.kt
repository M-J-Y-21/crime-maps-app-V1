package com.rkpandey.mymaps

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.rkpandey.mymaps.models.Place
import com.rkpandey.mymaps.models.UserMap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_create_place.*
import kotlinx.android.synthetic.main.dialog_create_place.view.*
import java.io.File

private const val TAG = "CreateMapActivity"
private const val FILE_NAME = "photo.jpg"
private const val REQUEST_CODE = 42
private lateinit var photoFile: File

class CreateMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var markers: MutableList<Marker> = mutableListOf()
    private val LOCATION_PERMISSION_REQUEST = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedPosition: Int = 0


    private fun getLocationAccess() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

        } else ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_map)



        supportActionBar?.title = intent.getStringExtra(EXTRA_MAP_TITLE)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapFragment.view?.let {
            Snackbar.make(it, "Long press to add a marker!", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", {})
                .setActionTextColor(ContextCompat.getColor(this, android.R.color.white))
                .show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // TODO: Consider calling
            return
        }


    }

    private fun getPhotoFile(fileName: String): File {
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            val takenImage = data?.extras?.get("data") as Bitmap
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageView.setImageBitmap(takenImage)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) { // TODO: Consider calling
                    return
                }
                mMap.isMyLocationEnabled = true

            } else {
                Toast.makeText(
                    this,
                    "User has not granted location access permission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_map, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Check that `item` is the save menu option
        if (item.itemId == R.id.miSave) {
            Log.i(TAG, "Tapped on save!")
            if (markers.isEmpty()) {
                Toast.makeText(
                    this,
                    "There must be at least one marker on the map",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
            val places = markers.map { marker ->
                Place(
                    marker.title,
                    marker.snippet,
                    marker.position.latitude,
                    marker.position.longitude,
                    215F
                )
            }
            val userMap = UserMap(intent.getStringExtra(EXTRA_MAP_TITLE), places)
            val data = Intent()
            data.putExtra(EXTRA_USER_MAP, userMap)
            setResult(Activity.RESULT_OK, data)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.setOnInfoWindowClickListener { markerToDelete ->
            Log.i(TAG, "onWindowClickListener- delete this marker")
            markers.remove(markerToDelete)
            markerToDelete.remove()
        }

        mMap.setOnMapLongClickListener { latLng ->
            Log.i(TAG, "onMapLongClickListener")
            showAlertDialog(latLng)

        }
        getLocationAccess() //        val userPos = getLocationAccess() Need to get users position for camera to pan to
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // TODO: Consider calling
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? -> // Got last known location. In some rare situations this can be null.
            location?.let {
                val mCurrentPlace = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentPlace, 15f))

//                mMap.setInfoWindowAdapter(CustomMarkerInfoWindowAdapter(this))
            }

        }


    }

    private fun showAlertDialog(latLng: LatLng) {
        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_place, null)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Create a marker")
                .setView(placeFormView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        placeFormView.btnTakePicture.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)

            // This DOESN'T work for API >= 24 (starting 2016)
            // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)

            val fileProvider =
                FileProvider.getUriForFile(this, "com.rkpandey.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_CODE)
            } else {
                Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()
            }
        }



        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = placeFormView.findViewById<EditText>(R.id.etTitle).text.toString()
            val description =
                placeFormView.findViewById<EditText>(R.id.etDescription).text.toString()
            if (title.trim().isEmpty() || description.trim().isEmpty()) {
                Toast.makeText(
                    this,
                    "Place must have non-empty title and description",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val spinner = dialog.findViewById<Spinner>(R.id.crimeSpinner)
            ArrayAdapter.createFromResource(
                this,
                R.array.crime_types,
                android.R.layout.simple_spinner_item
            ).also { adapter -> // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Apply the adapter to the spinner
                spinner!!.adapter = adapter
                spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        Toast.makeText(
                            parent?.context,
                            "Must Select A Crime To Add",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedPosition = position

                        // Getting the selected crime
                        val crimeName = adapterView?.getItemAtPosition(position).toString()

                        // Showing selected item
                        Toast.makeText(
                            adapterView?.context,
                            "Selected Crime: $crimeName",
                            Toast.LENGTH_SHORT
                        ).show()
                        spinner.onItemSelectedListener = this

                        when (selectedPosition) {
                            0 -> {
                                Toast.makeText(
                                    this@CreateMapActivity,
                                    "You Must Select A Valid Crime To Add!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            1 -> {
                                val marker = mMap.addMarker(
                                    MarkerOptions().position(latLng).title(title)
                                        .snippet(description)
                                        .draggable(true).icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED
                                            )
                                        )
                                )

                                markers.add(marker)
                                dialog.dismiss()


                            }

                            2 -> {
                                val marker = mMap.addMarker(
                                    MarkerOptions().position(latLng).title(title)
                                        .snippet(description)
                                        .draggable(true).icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_AZURE
                                            )
                                        )
                                )

                                markers.add(marker)
                                dialog.dismiss()

                            }

                            3 -> {
                                val marker = mMap.addMarker(
                                    MarkerOptions().position(latLng).title(title)
                                        .snippet(description)
                                        .draggable(true).icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_BLUE
                                            )
                                        )
                                )

                                markers.add(marker)
                                dialog.dismiss()


                            }

                            4 -> {
                                val marker = mMap.addMarker(
                                    MarkerOptions().position(latLng).title(title)
                                        .snippet(description)
                                        .draggable(true).icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_CYAN
                                            )
                                        )
                                )

                                markers.add(marker)
                                dialog.dismiss()


                            }

                            5 -> {
                                val marker = mMap.addMarker(
                                    MarkerOptions().position(latLng).title(title)
                                        .snippet(description)
                                        .draggable(true).icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_GREEN
                                            )
                                        )
                                )

                                markers.add(marker)
                                dialog.dismiss()


                            }

                        }

                    }

                }
            }
        }

    }

}