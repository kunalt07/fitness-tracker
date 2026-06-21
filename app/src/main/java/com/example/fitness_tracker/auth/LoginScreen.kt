package com.example.fitness_tracker.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.R
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillCta

@Composable
fun WelcomeScreen(
    viewModel: AuthViewModel = viewModel(),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    val canContinue = name.trim().isNotBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Brand mark — uses the actual launcher artwork.
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Vector",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Light,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tell us a bit about you to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            MinimalTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = "Your name",
                placeholder = "What should we call you?",
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MinimalTextField(
                value = email,
                onValueChange = { email = it.take(80) },
                label = "Email (optional)",
                placeholder = "you@example.com",
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(32.dp))

            PillCta(
                label = "Get started",
                enabled = canContinue,
                onClick = { viewModel.saveProfile(name, email) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Everything stays on your device. No account required.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
