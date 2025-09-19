package com.paoapps.blockedcache.sample.android.composeui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.paoapps.blockedcache.sample.viewmodel.HomeViewModel
import org.koin.androidx.compose.getViewModel

class AndroidHomeViewModel(
    private val viewModel: HomeViewModel
): ViewModel() {
    val output = viewModel.output

    fun refresh() {
        viewModel.refresh()
    }
}

@Composable
fun HomeView(
    modifier: Modifier = Modifier,
    viewModel: AndroidHomeViewModel = getViewModel(),
    openDetail: (Int) -> Unit
) {
    val output by viewModel.output.collectAsState(initial = null)
    output?.let { output ->

        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(text = output.title)

            Button(onClick = {
                 viewModel.refresh()
            }) {
                Text(text = "Refresh")
            }

            output.items.forEach {
                Text(text = it.title)
                Text(text = it.description)
                Button(onClick = {
                    openDetail(it.id)
                }) {
                    Text(text = "Open Detail")
                }
            }
        }
    }
}
