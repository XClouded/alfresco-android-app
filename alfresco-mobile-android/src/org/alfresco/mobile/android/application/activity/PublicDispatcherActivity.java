/*******************************************************************************
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 * 
 * This file is part of Alfresco Mobile for Android.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.alfresco.mobile.android.application.activity;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.alfresco.mobile.android.api.model.Document;
import org.alfresco.mobile.android.api.model.ListingFilter;
import org.alfresco.mobile.android.api.model.Node;
import org.alfresco.mobile.android.api.model.Task;
import org.alfresco.mobile.android.api.services.CommentService;
import org.alfresco.mobile.android.api.services.WorkflowService;
import org.alfresco.mobile.android.api.session.CloudSession;
import org.alfresco.mobile.android.api.session.RepositorySession;
import org.alfresco.mobile.android.application.R;
import org.alfresco.mobile.android.application.accounts.AccountManager;
import org.alfresco.mobile.android.application.accounts.fragment.AccountOAuthFragment;
import org.alfresco.mobile.android.application.fragments.DisplayUtils;
import org.alfresco.mobile.android.application.fragments.FragmentDisplayer;
import org.alfresco.mobile.android.application.fragments.ListingModeFragment;
import org.alfresco.mobile.android.application.fragments.browser.ChildrenBrowserFragment;
import org.alfresco.mobile.android.application.fragments.favorites.FavoritesFragment;
import org.alfresco.mobile.android.application.fragments.favorites.FavoritesSyncFragment;
import org.alfresco.mobile.android.application.fragments.fileexplorer.FileExplorerFragment;
import org.alfresco.mobile.android.application.fragments.menu.MenuActionItem;
import org.alfresco.mobile.android.application.fragments.operations.OperationsFragment;
import org.alfresco.mobile.android.application.fragments.properties.DetailsFragment;
import org.alfresco.mobile.android.application.fragments.sites.BrowserSitesFragment;
import org.alfresco.mobile.android.application.fragments.upload.UploadFormFragment;
import org.alfresco.mobile.android.application.fragments.workflow.task.TaskDetailsFragment;
import org.alfresco.mobile.android.application.fragments.workflow.task.TasksFragment;
import org.alfresco.mobile.android.application.intent.IntentIntegrator;
import org.alfresco.mobile.android.application.preferences.PasscodePreferences;
import org.alfresco.mobile.android.application.security.PassCodeActivity;
import org.alfresco.mobile.android.application.utils.SessionUtils;
import org.alfresco.mobile.android.application.utils.UIUtils;
import org.alfresco.mobile.android.ui.fragments.BaseFragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Activity responsible to manage public intent from 3rd party application. This activity is "open" to public Intent.
 * 
 * @author Jean Marie Pascal
 */
public class PublicDispatcherActivity extends BaseActivity implements OnInitListener
{
    private static final String TAG = PublicDispatcherActivity.class.getName();
    
    private boolean ready = false;
    private TextToSpeech ttobj;
    
    /** Define the type of importFolder. */
    private int uploadFolder;

    /** Define the local file to upload */
    private List<File> uploadFiles;

    private boolean activateCheckPasscode = false;

    private PublicDispatcherActivityReceiver receiver;

    protected long requestedAccountId = -1;

    // ///////////////////////////////////////////////////////////////////////////
    // LIFECYCLE
    // ///////////////////////////////////////////////////////////////////////////
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Intent intent = getIntent();
        
        activateCheckPasscode = false;

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();

        int[] values = UIUtils.getScreenDimension(this);
        int height = values[1];
        int width = values[0];

        params.height = (int) Math.round(height * 0.9);
        params.width = (int) Math
                .round(width
                        * (Float.parseFloat(getResources().getString(android.R.dimen.dialog_min_width_minor).replace(
                                "%", "")) * 0.01));

        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        setContentView(R.layout.app_left_panel);

        String action = intent.getAction();
        if ((Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) && getFragment(UploadFormFragment.TAG) == null)
        {
            Fragment f = new UploadFormFragment();
            FragmentDisplayer.replaceFragment(this, f, DisplayUtils.getLeftFragmentId(this), UploadFormFragment.TAG,
                    false, false);
            return;
        }

        if (IntentIntegrator.ACTION_SYNCHRO_DISPLAY.equals(action))
        {
            Fragment f = FavoritesSyncFragment.newInstance(FavoritesSyncFragment.MODE_PROGRESS);
            FragmentDisplayer.replaceFragment(this, f, DisplayUtils.getLeftFragmentId(this), OperationsFragment.TAG,
                    false, false);
            return;
        }

        if (IntentIntegrator.ACTION_PICK_FILE.equals(action))
        {
            if (intent.hasExtra(IntentIntegrator.EXTRA_ACCOUNT_ID))
            {
                currentAccount = AccountManager.retrieveAccount(this, intent.getLongExtra(IntentIntegrator.EXTRA_ACCOUNT_ID, 1));
            }
            
            File f = Environment.getExternalStorageDirectory();
            if (intent.hasExtra(IntentIntegrator.EXTRA_FOLDER))
            {
                f = (File) intent.getExtras().getSerializable(IntentIntegrator.EXTRA_FOLDER);
                Fragment fragment = FileExplorerFragment.newInstance(f, ListingModeFragment.MODE_PICK, true, 1);
                FragmentDisplayer.replaceFragment(this, fragment, DisplayUtils.getLeftFragmentId(this),
                        FileExplorerFragment.TAG, false, false);
            }
            
            return;
        }
        
        if (IntentIntegrator.ACTION_DISPLAY_NODE.equals(action))
        {
            if (intent.hasExtra(IntentIntegrator.EXTRA_ACCOUNT_ID))
            {
                currentAccount = AccountManager.retrieveAccount(this, intent.getLongExtra(IntentIntegrator.EXTRA_ACCOUNT_ID, 1));
            }
            
            Node node = (Node)intent.getExtras().getParcelable(IntentIntegrator.EXTRA_NODE);

            BaseFragment frag = DetailsFragment.newInstance((Document)node);
            frag.setSession(SessionUtils.getSession(this));
            FragmentDisplayer.replaceFragment(this, frag, getFragmentPlace(), DetailsFragment.TAG, false);
            
            return;
        }
        
        if (IntentIntegrator.ACTION_DISPLAY_TASK.equals(action) ||
            IntentIntegrator.ACTION_DISPLAY_TASK_ATTS.equals(action))
        {
            Task task = (Task)intent.getExtras().getSerializable(IntentIntegrator.EXTRA_TASK);
            Process process = (Process)intent.getExtras().getSerializable(IntentIntegrator.EXTRA_PROCESS);
            
            BaseFragment frag = TaskDetailsFragment.newInstance(task);
            frag.setSession(SessionUtils.getSession(this));
            FragmentDisplayer.replaceFragment(this, frag, getFragmentPlace(), TaskDetailsFragment.TAG, false);
            
            //This code is WIP.
            //NOTE: The TTS service doesn't function properly on Android 4.4.2, hence some experimental code here!
            ttobj = new TextToSpeech(this, this);               
            new Thread()
            {
                @Override
                public void run()
                {
                    //while (ttobj == null || ttobj.getDefaultEngine() == null || ttobj.getDefaultEngine().length() == 0)
                    {
                        try {Thread.sleep(4000);} catch(Exception e) {}
                    }
                    
                    ttobj.setLanguage(Locale.UK);
                    
                    ttobj.speak("Hello there, this is a test of the speech synthesis.  OK.  OK", TextToSpeech.QUEUE_FLUSH, null);
                    
                    super.run();
                }
                
            }.start();
            
            return;
        }
        
        String reply = intent.getStringExtra(IntentIntegrator.EXTRA_VOICE_REPLY);
        if (!TextUtils.isEmpty(reply))
        {
            Node node = (Node)intent.getExtras().getSerializable(IntentIntegrator.EXTRA_NODE);
            Task task = (Task)intent.getExtras().getSerializable(IntentIntegrator.EXTRA_TASK);
            
            if (node != null)
            {
                CommentService cs = getCurrentSession().getServiceRegistry().getCommentService();
                
                cs.addComment(node, reply);
            }
            else
            if (task != null)
            {
                
            }
            
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PassCodeActivity.REQUEST_CODE_PASSCODE)
        {
            if (resultCode == RESULT_CANCELED)
            {
                finish();
            }
            else
            {
                activateCheckPasscode = true;
            }
        }
    }

    @Override
    protected void onStart()
    {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (receiver == null)
        {
            receiver = new PublicDispatcherActivityReceiver();
            IntentFilter filters = new IntentFilter(IntentIntegrator.ACTION_LOAD_ACCOUNT_ERROR);
            filters.addAction(IntentIntegrator.ACTION_LOAD_ACCOUNT);
            filters.addAction(IntentIntegrator.ACTION_LOAD_ACCOUNT_COMPLETED);
            broadcastManager.registerReceiver(receiver, filters);
        }

        super.onStart();
        PassCodeActivity.requestUserPasscode(this);
        activateCheckPasscode = PasscodePreferences.hasPasscodeEnable(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (!activateCheckPasscode)
        {
            PasscodePreferences.updateLastActivityDisplay(this);
        }
    }

    @Override
    protected void onStop()
    {
        if (receiver != null)
        {
            broadcastManager.unregisterReceiver(receiver);
        }
        super.onStop();
    }

    // ///////////////////////////////////////////////////////////////////////////
    // UI Public Method
    // ///////////////////////////////////////////////////////////////////////////
    public void doCancel(View v)
    {
        finish();
    }

    public void validateAction(View v)
    {
        ((ChildrenBrowserFragment) getFragment(ChildrenBrowserFragment.TAG)).createFiles(uploadFiles);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // MENU
    // ///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        if (isVisible(ChildrenBrowserFragment.TAG))
        {
            ((ChildrenBrowserFragment) getFragment(ChildrenBrowserFragment.TAG)).getMenu(menu);
            return true;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MenuActionItem.MENU_CREATE_FOLDER:
                ((ChildrenBrowserFragment) getFragment(ChildrenBrowserFragment.TAG)).createFolder();
                return true;
            case android.R.id.home:
                if (getIntent() != null && IntentIntegrator.ACTION_PICK_FILE.equals(getIntent().getAction()))
                {
                    finish();
                }
                else
                {
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // UTILS
    // ///////////////////////////////////////////////////////////////////////////
    public void setUploadFolder(int uploadFolderType)
    {
        this.uploadFolder = uploadFolderType;
    }

    public void setUploadFile(List<File> localFile)
    {
        this.uploadFiles = localFile;
    }
    // ////////////////////////////////////////////////////////
    // BROADCAST RECEIVER
    // ///////////////////////////////////////////////////////
    private class PublicDispatcherActivityReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, intent.getAction());

            Activity activity = PublicDispatcherActivity.this;

            // During the session creation, display a waiting dialog.
            if (IntentIntegrator.ACTION_LOAD_ACCOUNT.equals(intent.getAction()))
            {
                if (!intent.hasExtra(IntentIntegrator.EXTRA_ACCOUNT_ID)) { return; }
                requestedAccountId = intent.getExtras().getLong(IntentIntegrator.EXTRA_ACCOUNT_ID);
                displayWaitingDialog();
                return;
            }

            // If the sesison is available, display the view associated (repository, sites, downloads, favorites).
            if (IntentIntegrator.ACTION_LOAD_ACCOUNT_COMPLETED.equals(intent.getAction()))
            {
                if (!intent.hasExtra(IntentIntegrator.EXTRA_ACCOUNT_ID)) { return; }
                long accountId = intent.getExtras().getLong(IntentIntegrator.EXTRA_ACCOUNT_ID);
                if (requestedAccountId != -1 && requestedAccountId != accountId) { return; }
                requestedAccountId = -1;

                setProgressBarIndeterminateVisibility(false);

                if (getCurrentSession() instanceof RepositorySession)
                {
                    DisplayUtils.switchSingleOrTwo(activity, false);
                }
                else if (getCurrentSession() instanceof CloudSession)
                {
                    DisplayUtils.switchSingleOrTwo(activity, true);
                }

                // Remove OAuthFragment if one
                if (getFragment(AccountOAuthFragment.TAG) != null)
                {
                    getFragmentManager().popBackStack(AccountOAuthFragment.TAG,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                removeWaitingDialog();

                // Upload process : Display the view where the user wants to upload files.
                BaseFragment frag = null;
                if (getCurrentSession() != null && uploadFolder == R.string.menu_browse_sites)
                {
                    frag = BrowserSitesFragment.newInstance();
                    FragmentDisplayer.replaceFragment(activity, frag, DisplayUtils.getLeftFragmentId(activity),
                            BrowserSitesFragment.TAG, true);
                }
                else if (getCurrentSession() != null && uploadFolder == R.string.menu_browse_root)
                {
                    addNavigationFragment(getCurrentSession().getRootFolder());
                }
                else if (getCurrentSession() != null && uploadFolder == R.string.menu_favorites_folder)
                {
                    frag = FavoritesFragment.newInstance(FavoritesFragment.MODE_FOLDERS);
                    FragmentDisplayer.replaceFragment(activity, frag, DisplayUtils.getLeftFragmentId(activity),
                            FavoritesFragment.TAG, true);
                }
                return;
            }
        }
    }
    
    @Override
    public void onInit(int status)
    {
       if (status != TextToSpeech.ERROR)
       {
           //ttobj.setLanguage(Locale.UK);
           //ready = true;
           
           //ttobj.speak("Hello there", TextToSpeech.QUEUE_FLUSH, null);
       }     
    }
}
