package com.paoapps.blockedcache.sample.android.composeui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.paoapps.blockedcache.sample.viewmodel.CoffeeDetailViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

class AndroidCoffeeDetailViewModel(
    private val viewModel: CoffeeDetailViewModel
): ViewModel() {
    val output = viewModel.output
}

@Composable
fun CoffeeDetailView(
    modifier: Modifier = Modifier,
    id: Int,
    viewModel: AndroidCoffeeDetailViewModel = getViewModel { parametersOf(id) }
) {
    val output by viewModel.output.collectAsState(initial = null)

    output?.let { output ->
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(text = output.title)
            Text(text = output.description)
            Text(text = output.ingredients)
        }
    }
}
