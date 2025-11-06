package com.example.structurescan
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class Onboarding3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Onboarding3Screen(
                    onNext = {
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                    },
                    onSkip = {
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                    },
                    onBack = {
                        val intent = Intent(this, Onboarding2Activity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun Onboarding3Screen(onNext: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // Top bar with back + skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Gray
                )
            }
            Text(
                text = "Skip",
                color = Color(0xFF000000),
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSkip() }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Illustration inside circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color(0xFFE3F2FD), shape = CircleShape) // light blue bg
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.onboarding3), // replace with your image
                contentDescription = "Phone icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Get Clear Recommendations",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0288D1),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )


        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = "Receive personalized safety recommendations and track changes over time to ensure your building remains safe.",
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Pager indicator
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.LightGray, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.LightGray, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF0288D1), shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next button
        Button(
            onClick = { onNext() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Get Started", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun Onboarding3Preview() {
    MaterialTheme {
        Onboarding3Screen(onNext = {}, onSkip = {}, onBack = {})
    }
}