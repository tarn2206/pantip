package tarn.pantip.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Account;
import tarn.pantip.model.User;
import tarn.pantip.util.ApiAware;
import tarn.pantip.util.Utils;

/**
 * User: Tarn
 * Date: 4/29/13 9:48 PM
 */
public class LoginActivity extends AppCompatActivity implements TextWatcher, TextView.OnEditorActionListener, View.OnClickListener
{
    private View loginGroup;
    private View progressGroup;
    private EditText loginName;
    private EditText password;
    private Button loginButton;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ApiAware.setTaskDescription(this, null);

        loginGroup = findViewById(R.id.login_main_group);
        if (Pantip.displayWidth > Utils.toPixels(440)) loginGroup.getLayoutParams().width = Utils.toPixels(400);
        progressGroup = findViewById(R.id.login_progress_group);

        String lastUser = Pantip.getSharedPreferences().getString("last_user", "");
        loginName = findViewById(R.id.login_name);
        loginName.setText(lastUser);
        loginName.addTextChangedListener(this);
        password = findViewById(R.id.login_password);
        password.setOnEditorActionListener(this);
        password.addTextChangedListener(this);

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);
        if (getResources().getDisplayMetrics().widthPixels > 640)
            loginButton.getLayoutParams().width = 640;
        /*TextView register = findViewById(R.id.register);
        Utils.addLinkMovementMethod(register);*/
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    { }

    @Override
    public void afterTextChanged(Editable s)
    {
        String userName = loginName.getText().toString().trim();
        String password = this.password.getText().toString().trim();
        loginButton.setEnabled(userName.length() > 0 && password.length() > 0);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (actionId == EditorInfo.IME_ACTION_DONE)
        {
            login();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        login();
    }

    private void login()
    {
        Utils.hideKeyboard(this);

        String userName = loginName.getText().toString().trim();
        String password = this.password.getText().toString().trim();

        Pantip.getSharedPreferences().edit().putString("last_user", userName).apply();
        loginGroup.setVisibility(View.GONE);
        progressGroup.setVisibility(View.VISIBLE);

        Account.login(userName, password).subscribe(this::complete, tr -> {
            loginGroup.setVisibility(View.VISIBLE);
            progressGroup.setVisibility(View.INVISIBLE);
            Pantip.handleException(this, tr);
        });
    }

    private void complete(User user)
    {
        if (user != null && user.name != null)
        {
            if (user.error != null) Utils.showToast(user.error);
            Pantip.currentUser = user;
            Pantip.loggedOn = true;
            Pantip.getSharedPreferences()
                    .edit()
                    .putString("user_name", user.name)
                    .putInt("mid", user.id)
                    .putString("avatar", user.avatar)
                    .apply();
            setResult(RESULT_OK);
            finish();
        }
        else
        {
            Pantip.loggedOn = false;
            Utils.createDialog(this)
                 .setMessage(user == null ? "login failed" : user.error)
                 .setPositiveButton("ตกลง", (dialog, which) -> {
                     loginGroup.setVisibility(View.VISIBLE);
                     progressGroup.setVisibility(View.INVISIBLE);
                 })
                 .show();
        }
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}