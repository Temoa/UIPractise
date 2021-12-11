package me.temoa.uipractise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.temoa.uipractise.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.arcSeekBar.builder.setMax(120).setMin(60).build()
    binding.arcSeekBar.setOnProgressChangeListener(object : ArcSeekBar.OnProgressChangeListener {

      override fun onProgressChanged(seekBar: ArcSeekBar, progress: Int, isUser: Boolean) {
        binding.text.text = progress.toString()
        println(progress)
      }

      override fun onStartTrackingTouch(seekBar: ArcSeekBar) {
        println("onStartTrackingTouch")
      }

      override fun onStopTrackingTouch(seekBar: ArcSeekBar) {
        println("onStopTrackingTouch")
      }
    })
    binding.arcSeekBar.setProgress(80)
  }
}