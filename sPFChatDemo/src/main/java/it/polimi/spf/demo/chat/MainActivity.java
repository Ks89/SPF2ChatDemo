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

import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import butterknife.Bind;
import butterknife.ButterKnife;
import it.polimi.spf.lib.SPFPermissionManager;
import it.polimi.spf.lib.services.SPFServiceRegistry;
import it.polimi.spf.shared.model.Permission;
import it.polimi.spf.shared.model.SPFActivity;
import it.polimi.spf.shared.model.SPFError;

public class MainActivity extends ToolbarActivity {

    private static final String TAG = "MainActivity";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Observable mMessageNotifier = new Observable() {
        @Override
        public boolean hasChanged() {
            return true;
        }
    };

    @Bind(R.id.tab_layout)
    TabLayout tabLayout;
    @Bind(R.id.view_pager)
    ViewPager mViewPager;
    @Bind(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        super.setupToolbar(toolbar, R.string.app_name, R.color.toolbar_text_color);

        SPFPermissionManager.get().requirePermission(Permission.READ_LOCAL_PROFILE,
                Permission.READ_REMOTE_PROFILES, Permission.SEARCH_SERVICE, Permission.REGISTER_SERVICES,
                Permission.ACTIVITY_SERVICE);


        String[] mPageTitles = this.getResources().getStringArray(R.array.main_section_titles);

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(mPageTitles[0].toUpperCase(Locale.getDefault())));
        tabLayout.addTab(tabLayout.newTab().setText(mPageTitles[1].toUpperCase(Locale.getDefault())));
        tabLayout.addTab(tabLayout.newTab().setText(mPageTitles[2].toUpperCase(Locale.getDefault())));

        MainPagerAdapter mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.setTabMode(TabLayout.MODE_FIXED);
                tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            }
        });

        Log.d(TAG, "ServiceComponentName: \"" + new ComponentName(this, ProximityServiceImpl.class).flattenToString() + "\"");

        SPFServiceRegistry.load(this, new SPFServiceRegistry.Callback() {
            @Override
            public void onServiceReady(SPFServiceRegistry localServiceRegistry) {
                localServiceRegistry.registerService(ProximityService.class, ProximityServiceImpl.class);
                localServiceRegistry.disconnect();
            }

            @Override
            public void onError(SPFError spfError) {
                if (spfError.getCode() == SPFError.SPF_NOT_INSTALLED_ERROR_CODE) {
                    onSPFNotInstalled();
                }
                Log.e(TAG, "Could not register service: " + spfError);
            }

            @Override
            public void onDisconnect() {
                Log.d(TAG, "Disconnected from SPF");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProximityServiceImpl.setEventListener(mProximityEventListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ProximityServiceImpl.removeEventListener(mProximityEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    ProximityServiceImpl.EventListener mProximityEventListener = new ProximityServiceImpl.EventListener() {
        @Override
        public void onPokeReceived(final SPFActivity poke) {
            uiHandler.post(new Runnable() {

                @Override
                public void run() {
                    String senderName = poke.get(SPFActivity.SENDER_DISPLAY_NAME);
                    Toast.makeText(MainActivity.this, senderName + " poked you!", Toast.LENGTH_LONG).show();
                }

            });
        }

        @Override
        public void onMessageReceived(final SPFActivity message) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageNotifier.notifyObservers(message);
                }
            });
        }
    };

    public void registerMessageObserver(Observer obs) {
        mMessageNotifier.addObserver(obs);
    }

    public void unregisterMessageObserver(Observer mMessageObserver) {
        mMessageNotifier.deleteObserver(mMessageObserver);
    }

    private void onSPFNotInstalled() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_spf_not_installed_title)
                .setMessage(R.string.error_spf_not_installed_msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(1);
                    }
                }).show();
    }

    private static class MainPagerAdapter extends FragmentPagerAdapter {
        private int mNumOfTabs;

        private MainPagerAdapter(FragmentManager fm, int mNumOfTabs) {
            super(fm);
            this.mNumOfTabs = mNumOfTabs;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new PeopleFragment();
                case 1:
                    return new ConversationListFragment();
                case 2:
                    return ProfileFragment.forSelfProfile();

                default:
                    throw new IndexOutOfBoundsException("Requested page " + i + ", total " + mNumOfTabs);
            }
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }
    }
}
