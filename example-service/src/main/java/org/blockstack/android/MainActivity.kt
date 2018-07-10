package org.blockstack.android


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.UserData


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        signInButton.isEnabled = false
        startServiceButton.isEnabled = false

        _blockstackSession = BlockstackSession(this, defaultConfig,
                onLoadedCallback = {
                    // Wait until this callback fires before using any of the
                    // BlockstackSession API methods
                    _blockstackSession?.isUserSignedIn { signedIn ->
                        if (signedIn) {
                            _blockstackSession?.loadUserData { it?.let { onSignIn(it) } }
                        } else {
                            signInButton.isEnabled = true
                            startServiceButton.isEnabled = false
                        }
                    }
                })

        signInButton.setOnClickListener { view: View ->
            blockstackSession().redirectUserToSignIn { userData ->
                Log.d(TAG, "signed in!")
                runOnUiThread {
                    onSignIn(userData)
                }
            }
        }

        startServiceButton.setOnClickListener { _ ->
            startService(Intent(this, BlockstackService::class.java))
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun onSignIn(userData: UserData) {
        signInButton.isEnabled = false
        startServiceButton.isEnabled = true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            blockstackSession().loadUserData { userData ->
                runOnUiThread {
                    if (userData != null) {
                        onSignIn(userData)
                    }
                }
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun handleAuthResponse(intent: Intent) {
        val response = intent.dataString
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split(':')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse, { userData ->
                    Log.d(TAG, "signed in!")
                    runOnUiThread {
                        onSignIn(userData)
                    }
                })
            }
        }
    }

    fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

}
