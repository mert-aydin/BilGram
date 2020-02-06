package com.mert.bilgram

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignInFragment : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)
        emailET = view.findViewById(R.id.mailET)
        passwordET = view.findViewById(R.id.passwordET)
        signIn = view.findViewById(R.id.sign_in_btn)
        signIn.setOnClickListener { signIn() }
        emailET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                signIn.isEnabled = !(charSequence.toString().trim { it <= ' ' }.isEmpty() || passwordET.text.toString().length < 6)
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        passwordET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                signIn.isEnabled = !(charSequence.toString().trim { it <= ' ' }.length < 6 || emailET.text.toString().isEmpty())
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        return view
    }

    private fun signIn() {
        hideKeyboard(activity!!)
        mAuth.signInWithEmailAndPassword(emailET.text.toString(), passwordET.text.toString())
                .addOnCompleteListener(activity!!) { task ->
                    if (task.isSuccessful) startActivity(Intent(context, FeedActivity::class.java))

                }.addOnFailureListener(activity!!) { e -> Snackbar.make(signIn, e.localizedMessage!!, Snackbar.LENGTH_LONG).show() }
    }

    companion object {
        private lateinit var emailET: EditText
        private lateinit var passwordET: EditText
        private lateinit var signIn: Button

        private fun hideKeyboard(activity: Activity) {
            var view = activity.currentFocus
            if (view == null) view = View(activity)
            (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}