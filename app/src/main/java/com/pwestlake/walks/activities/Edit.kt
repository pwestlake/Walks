package com.pwestlake.walks.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.app.NavUtils
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.pwestlake.walks.R
import com.pwestlake.walks.bo.WalkMetaData
import com.pwestlake.walks.databinding.ActivityEditBinding
import java.text.SimpleDateFormat

class Edit : AppCompatActivity() {
    var item = ObservableField<WalkMetaData>()
    val date = ObservableField<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val binding: ActivityEditBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_edit)
        binding.edit = this

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val b = getIntent().getExtras();
        if (b != null) {
            item.set(b.getParcelable<WalkMetaData>("item"))

            val dfmt = SimpleDateFormat("dd MMM yyyy HH:mm")
            date.set(dfmt.format(item.get()?.date))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
            {
                val intent = NavUtils.getParentActivityIntent(this);
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                if (intent != null) {
                    createResult(intent)
                }

                if (intent != null) {
                    NavUtils.navigateUpTo(this, intent)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        val data = Intent()
        createResult(data)
        super.finish();
    }

    private fun createResult(intent: Intent) {
        intent.putExtra("item", item.get())

        setResult(0, intent);
    }
}
