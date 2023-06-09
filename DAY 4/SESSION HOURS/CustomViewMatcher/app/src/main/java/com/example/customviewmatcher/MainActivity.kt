package com.example.customviewmatcher



import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity


/**
 * A login screen that offers login via email/password.
 */
class MainActivity : AppCompatActivity(),
    LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    // UI references.
    private var mEmailView: AutoCompleteTextView? = null
    private var mPasswordView: EditText? = null
    private var mProgressView: View? = null
    private var mLoginFormView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the login form.
        mEmailView = findViewById<View>(R.id.email) as AutoCompleteTextView
        //populateAutoComplete()
        mPasswordView = findViewById<View>(R.id.password) as EditText
        mPasswordView!!.setOnEditorActionListener(OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        val mEmailSignInButton = findViewById<View>(R.id.email_sign_in_button) as Button
        mEmailSignInButton.setOnClickListener { attemptLogin() }
        mLoginFormView = findViewById(R.id.login_form)
        mProgressView = findViewById(R.id.login_progress)
    }

    private fun populateAutoComplete() {
        loaderManager?.initLoader(0, null, this)
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mEmailView!!.error = null
        mPasswordView!!.error = null

        // Store values at the time of the login attempt.
        val email = mEmailView!!.text.toString()
        val password = mPasswordView!!.text.toString()
        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView!!.error = getString(R.string.error_invalid_password)
            focusView = mPasswordView
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView!!.error = getString(R.string.error_field_required)
            focusView = mEmailView
            cancel = true
        } else if (!isEmailValid(email)) {
            mEmailView!!.error = getString(R.string.error_invalid_email)
            focusView = mEmailView
            cancel = true
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(email, password)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
        mLoginFormView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 0 else 1.toFloat()) as Float
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
            }
        })
        mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
        mProgressView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 1 else 0.toFloat()) as Float
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        return CursorLoader(
            this,  // Retrieve data rows for the device user's 'profile' contact.
            Uri.withAppendedPath(
                ContactsContract.Profile.CONTENT_URI,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY
            ),
            ProfileQuery.PROJECTION,  // Select only email addresses.
            ContactsContract.Contacts.Data.MIMETYPE +
                    " = ?",
            arrayOf(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),  // Show primary email addresses first. Note that there won't be
            // a primary email address if the user hasn't specified one.
            ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        )
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails: MutableList<String> = ArrayList()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }
        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {}
    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_dropdown_item_1line, emailAddressCollection
        )
        mEmailView!!.setAdapter(adapter)
    }

    private interface ProfileQuery {
        companion object {
            val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY
            )
            const val ADDRESS = 0
            const val IS_PRIMARY = 1
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(
        private val mEmail: String,
        private val mPassword: String
    ) :
        AsyncTask<Void?, Void?, Boolean>() {
        protected fun doInBackground(vararg params: Void): Boolean {
            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }
            for (credential in DUMMY_CREDENTIALS) {
                val pieces = credential.split(":").toTypedArray()
                if (pieces[0] == mEmail) {
                    // Account exists, return true if the password matches.
                    return pieces[1] == mPassword
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean) {
            mAuthTask = null
            showProgress(false)
            if (success) {
                finish()
            } else {
                mPasswordView!!.error = getString(R.string.error_incorrect_password)
                mPasswordView!!.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            TODO("Not yet implemented")
        }
    }

    companion object {
        /**
         * A dummy authentication store containing known user names and passwords.
         */
        private val DUMMY_CREDENTIALS = arrayOf(
            "foo@example.com:hello", "bar@example.com:world"
        )
    }
}