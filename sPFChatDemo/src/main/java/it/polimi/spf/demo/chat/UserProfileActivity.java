/*
 * Copyright 2014 Jacopo Aliprandi, Dario Archetti
 * Copyright 2015 Stefano Cappa
 *
 * This file is part of SPF.
 *
 * SPF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * SPF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPF.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.polimi.spf.demo.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_IDENTIFIER = "personIdentifier";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        if (savedInstanceState == null) {
            String personIdentifier = getIntent().getStringExtra(EXTRA_PERSON_IDENTIFIER);
            if (personIdentifier == null) {
                throw new IllegalStateException("Missing personIdentifier in intent");
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, ProfileFragment.forRemoteProfile(personIdentifier))
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
