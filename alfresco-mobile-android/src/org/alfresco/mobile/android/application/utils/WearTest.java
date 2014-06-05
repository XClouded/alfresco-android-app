package org.alfresco.mobile.android.application.utils;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.mobile.android.api.model.ContentStream;
import org.alfresco.mobile.android.api.model.Document;
import org.alfresco.mobile.android.api.model.Folder;
import org.alfresco.mobile.android.api.model.ListingContext;
import org.alfresco.mobile.android.api.model.ListingFilter;
import org.alfresco.mobile.android.api.model.Node;
import org.alfresco.mobile.android.api.model.PagingResult;
import org.alfresco.mobile.android.api.model.Person;
import org.alfresco.mobile.android.api.model.Task;
import org.alfresco.mobile.android.api.services.DocumentFolderService;
import org.alfresco.mobile.android.api.services.PersonService;
import org.alfresco.mobile.android.api.services.WorkflowService;
import org.alfresco.mobile.android.api.services.impl.AbstractPersonService;
import org.alfresco.mobile.android.api.session.AlfrescoSession;
import org.alfresco.mobile.android.application.R;
import org.alfresco.mobile.android.application.activity.PublicDispatcherActivity;
import org.alfresco.mobile.android.application.fragments.workflow.task.TasksAdapter;
import org.alfresco.mobile.android.application.intent.IntentIntegrator;
import org.alfresco.mobile.android.application.mimetype.MimeType;
import org.alfresco.mobile.android.application.mimetype.MimeTypeManager;
import org.alfresco.mobile.android.api.model.Process;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preview.support.wearable.notifications.*;
import android.preview.support.wearable.notifications.WearableNotifications.Action;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.text.Html;
import android.text.format.DateFormat;
 

public class WearTest
{
    private int notificationId = 460001;
    
    private AlfrescoSession session = null;
    private Activity activity = null;
    
    
    public WearTest(Activity mainActivity)
    {        
        activity = mainActivity;
        session = SessionUtils.getSession(activity);
    }
    
    public void nodeListener(Folder folder)
    {
        NodeWatcher nw = new NodeWatcher (true);
        nw.execute(folder);
    }
    
    public void taskListener()
    {
        TaskWatcher tw = new TaskWatcher();
        tw.execute(new Object[1]);        
    }

    
    private class NodeWatcher extends AsyncTask<Folder, Integer, Node>
    {
        private boolean repeat = false;
        private Folder folder = null;
        private DocumentFolderService service;
        
        public NodeWatcher(boolean repeat)
        {
            this.repeat = repeat;
            service = session.getServiceRegistry().getDocumentFolderService();
        }
        
        @Override
        protected Node doInBackground(Folder... home)
        {
            GregorianCalendar latestDate = null;
            List<Node> nodes;
            
            folder = home[0];
            
            /*
             * Probably don't need to check existing items, just go by time/date now.
             * 
             * 
             
            //Do initial scan for latest date.
            
            try { nodes = service.getChildren(home[0]); } catch (Exception e) { return null; }
            
            for (Node node : nodes)
            {
                GregorianCalendar date = node.getCreatedAt();
                
                if (latestDate == null)
                    latestDate = date;
                else
                if (date.after(latestDate))
                    latestDate = date;
            }
            */
            
            latestDate = new GregorianCalendar();
            
            //Wait for new nodes
            Node node = null;
            while (node == null)
            {
                //TODO: Make this a lot longer!
                try { Thread.sleep(5000); } catch (InterruptedException e) { return null; }
                
                try { nodes = service.getChildren(home[0]); } catch (Exception e) { return null; }
                
                for (Node n : nodes)
                {
                    GregorianCalendar date = n.getCreatedAt();
                    
                    if (latestDate == null)
                    {
                        node = n;
                        break;
                    }
                    else
                    if (date.after(latestDate))
                    {
                        node = n;
                        break;
                    }
                }  
            }
            
            return node;
        }
        
        @Override
        protected void onPostExecute(Node node)
        {
            MimeType mime = MimeTypeManager.getMimetype(activity, node.getName());
            int mimeIconId = mime.getSmallIconId(activity);
            int smallIconId = R.drawable.ic_notif_alfresco;
            //Bitmap largeIcon = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_alfresco_logo);
            
            // Build intent for notification content
            Intent viewIntent = new Intent(activity, PublicDispatcherActivity.class).setAction(IntentIntegrator.ACTION_DISPLAY_NODE);
            viewIntent.putExtra(IntentIntegrator.EXTRA_NODE, (Parcelable)node);
            
            PendingIntent viewPendingIntent = PendingIntent.getActivity(activity, 0, viewIntent, 0);
       
            // Build an intent for an action to view a map
            /*
            String label = "Node created here";
            String uriBegin = "geo:12,34";
            String query = "12,34(" + label + ")";
            String encodedQuery = Uri.encode( query  );
            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
            Uri geoUri = Uri.parse(uriBegin + "?q=" + encodedQuery);
            mapIntent.setData(geoUri);
            PendingIntent mapPendingIntent = PendingIntent.getActivity(activity, 0, mapIntent, 0);
            */
            
             // Specify the 'big view' content to display the long
             // event description that may not fit the normal content text.
             BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
             bigStyle.bigText("'" + node.getName() + "'was added to your folder by " + node.getModifiedBy() + " at " + 
                     node.getCreatedAt().get(GregorianCalendar.HOUR_OF_DAY) + ":" + node.getCreatedAt().get(GregorianCalendar.MINUTE) );
             
             // Key for the string that's delivered in the action's intent
             String[] replies = {"Thanks", "Will review"};
             RemoteInput remoteInput = new RemoteInput.Builder(IntentIntegrator.EXTRA_VOICE_REPLY)
                     .setLabel("Add a comment")
                     .setChoices(replies)
                     .build();
          
             // Create the notification action
             Intent replyIntent = new Intent(activity, PublicDispatcherActivity.class).setAction(IntentIntegrator.ACTION_REPLY_NODE);
             replyIntent.putExtra(IntentIntegrator.EXTRA_NODE, (Parcelable)node);
             PendingIntent replyPendingIntent = PendingIntent.getActivity(activity, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
             Action replyAction = new Action.Builder(mimeIconId, "Comment", replyPendingIntent).addRemoteInput(remoteInput).build();
            
             NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(activity)
                    .setSmallIcon(smallIconId)
                    .setContentTitle("New content added")
                    .setContentText(node.getName() + " was added to your folder")
                    .setContentIntent(viewPendingIntent)
                    //.addAction(android.R.drawable.ic_dialog_map, "Map", mapPendingIntent)
                    //.setLargeIcon(largeIcon)
                    .setStyle(bigStyle);
                    
            // Create wearable notification and add action
            Notification replyNotification = new WearableNotifications.Builder(notificationBuilder)//.setGroup("ALFRESCO_NODES")
                    .addAction(replyAction).build();
            
            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
       
            // Build the notification and issues it with notification manager.
            notificationManager.notify(notificationId++, replyNotification);
            
            if (repeat)
                new NodeWatcher(true).execute(folder);
            
            super.onPostExecute(node);
        }
    }
    
    
    private class TaskWatcher extends AsyncTask<Object, Integer, Boolean>
    {
        private WorkflowService service;
        private PersonService personService;
        private Map<String,Task> watchedTasks = new HashMap<String, Task>();
        
        
        public TaskWatcher()
        {
        }
        
        @Override
        protected Boolean doInBackground(Object... param)
        {
            while (session == null)
            {
                try { Thread.sleep(500); } catch (InterruptedException e) { return false; }
                
                session = SessionUtils.getSession(activity);
            }
            
            service = session.getServiceRegistry().getWorkflowService();
            personService = session.getServiceRegistry().getPersonService();
            
            if (service == null  ||  personService == null)
                return false;
            
            String currentUser = session.getPersonIdentifier();
            PagingResult<Task> activeTasks;
            PagingResult<Task> completedTasks;
            List<Task> tasks = new ArrayList<Task>();
            
            //Wait for new tasks
            while (true)
            {
                //TODO: Make this a lot longer!
                try { Thread.sleep(5000); } catch (InterruptedException e) { return false; }
                
                ListingContext lc;
                ListingFilter filter;
                
                try
                {
                    lc = new ListingContext();
                    filter = new ListingFilter();
                    filter.addFilter(WorkflowService.FILTER_KEY_STATUS, WorkflowService.FILTER_STATUS_ACTIVE);
                    lc.setFilter(filter);
                    activeTasks = service.getTasks(lc);
                    
                    lc = new ListingContext();
                    filter = new ListingFilter();
                    filter.addFilter(WorkflowService.FILTER_KEY_STATUS, WorkflowService.FILTER_STATUS_COMPLETE);
                    lc.setFilter(filter);
                    completedTasks = service.getTasks(lc);
                }
                catch (Exception e) { return false; }
                
                tasks.clear();
                tasks.addAll(activeTasks.getList());
                tasks.addAll(completedTasks.getList());
                
                for (Task t : tasks)
                {
                    Process p = service.getProcess(t.getProcessIdentifier());
                    Task watched = watchedTasks.get(t.getProcessIdentifier());
                    
                    if (watched == null)
                    {
                        if (t.getEndedAt() == null  && 
                            !t.getKey().contains("wf:approved")  &&
                            !t.getKey().contains("wf:completed") &&
                            !t.getKey().contains("wf:rejected")  &&
                            currentUser.equals(t.getAssigneeIdentifier()))
                        {
                            watchedTasks.put(t.getProcessIdentifier(), t);
                            generateNotification(t, p, "Task added",
                                                 "'" + p.getDescription() + "' has been added for your approval",
                                                 "'" + p.getDescription() + "' has been added for your approval by " +
                                                 p.getInitiatorIdentifier() + " on " +
                                                 DateFormat.getLongDateFormat(activity).format(p.getStartedAt().getTime()));
                        }
                    }
                    else
                    {
                        if (t.getEndedAt() != null  ||  t.getKey().contains("wf:completed"))
                        {
                            generateNotification(t, p, "Task completed",
                                                "'" + p.getDescription() + "' has been completed",
                                                "'" + p.getDescription() + "' has been completed");
                            
                            watchedTasks.remove(t.getProcessIdentifier());
                        } 
                        else
                        {
                            //Check for changes.  Some of these may not change, but the tests are here just in case.
                            
                            if (objectChanged(watched.getKey(), t.getKey()))
                            {
                                String status = null;
                                if (t.getKey().contains("wf:approved"))
                                    status = "approved";
                                else
                                if (t.getKey().contains("wf:completed"))
                                    status = "completed";
                                else
                                if (t.getKey().contains("wf:rejected"))
                                    status = "rejected";
                                
                                if (status != null)
                                    generateNotification(t, p, "Task " + status,
                                                        "'" + p.getDescription() + "' has been " + status,
                                                        "'" + p.getDescription() + "' has been " + status);
                            }
                            
                            if (watched.getPriority() != t.getPriority())
                                generateNotification(t, p, "Task priority changed",
                                                    "'" + p.getDescription() + "': Priority changed to " + t.getPriority(),
                                                    "'" + p.getDescription() + "': Priority changed to " + t.getPriority());
                                                        
                            //May occur if task expires without being actioned?
                            if (t.getEndedAt() != null)
                                generateNotification(t, p, "Task ended",
                                                    "'" + p.getDescription() + "' has ended",
                                                    "'" + p.getDescription() + "' has ended on " + 
                                                    DateFormat.getLongDateFormat(activity).format(t.getEndedAt().getTime()));
                            
                            if (objectChanged (watched.getDueAt(), t.getDueAt() ) )
                                generateNotification(t, p, "Task due-date changed",
                                                    "'" + p.getDescription() + "': Due date changed",
                                                    "'" + p.getDescription() + "': Due date changed to " + 
                                                    DateFormat.getLongDateFormat(activity).format(t.getDueAt().getTime()));
                            
                            //Take on any updates that have just been reported already.
                            watchedTasks.remove(t.getProcessIdentifier());
                            watchedTasks.put(t.getProcessIdentifier(), t);
                        }
                    }
                }  
            }
        }
        
        boolean objectChanged (Object d1, Object d2)
        {
            //d1 or d2 changed to be a valid object?
            if ( (d1 == null && d2 != null) || (d2 == null && d1 != null) )
                return true;
            else
            //d1 and d2 both null, ie. incomparable but same
            if (d1 == null && d2 == null)
                return false;
            else
                return (!d1.equals(d2));
        }
        
        void generateNotification(Task task, Process process, String title, String shortMessage, String longMessage)
        {
            int taskPriorityIconId = TasksAdapter.getPriorityIconId(task.getPriority());
            int smallIconId = R.drawable.ic_notif_alfresco;
            
            Bitmap largeIcon = Bitmap.createScaledBitmap (BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_alfresco_logo), 64, 64, false);
            try
            {
                 //SDK API broken for now, using backdoor...
                AbstractPersonService s = (AbstractPersonService)personService;
                ContentStream avatar = s.getAvatarStream(process.getInitiatorIdentifier());
                if (avatar != null)
                {
                    largeIcon = Bitmap.createScaledBitmap (BitmapFactory.decodeStream(avatar.getInputStream()), 64, 64, false);
                }                    
            }
            catch (Exception e) {}
                    
            
            // Build intent for notification content
            Intent viewIntent = new Intent(activity, PublicDispatcherActivity.class).setAction(IntentIntegrator.ACTION_DISPLAY_TASK);
            viewIntent.putExtra(IntentIntegrator.EXTRA_TASK, task);
            viewIntent.putExtra(IntentIntegrator.EXTRA_PROCESS, process);
            
            PendingIntent viewPendingIntent = PendingIntent.getActivity(activity, 0, viewIntent, 0);
                   
             // Specify the 'big view' content to display the long
             // event description that may not fit the normal content text.
             BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
             bigStyle.bigText(longMessage);
             
                          
             NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(activity)
                     .setLargeIcon(largeIcon)
                     .setSmallIcon(smallIconId)
                     .setContentTitle(title)
                     .setContentText(shortMessage)
                     .setContentIntent(viewPendingIntent)
                     .setStyle(bigStyle);
                 
             // Key for the string that's delivered in the action's intent
            ArrayList<String> replies = new ArrayList<String>();
            replies.add("Approve task");
            replies.add("Reject task");
            replies.add("Complete task");
            
            WearableNotifications.Builder replyNotificationBuilder = new WearableNotifications.Builder(notificationBuilder);
            
            List<Document> docs = null;
            try 
            {
                docs = service.getDocuments (task);
                
                //Document d = docs.get(0);
                //if (d != null)
                int maxPages = 3;
                for (Document d : docs)
                {
                    MimeType mime = MimeTypeManager.getMimetype(activity, d.getName());
                    int mimeIconId = mime.getSmallIconId(activity);
                    
                    if (maxPages == 0)
                        break;
                    
                    //Create a big text style for the second page
                    --maxPages;
                    BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
                    secondPageStyle.setBigContentTitle(d.getDescription() != null ? d.getDescription() : d.getName())
                                   .bigText(d.getDescription() != null ? d.getName()+". " : "" +
                                           "Created on " +
                                           DateFormat.getLongDateFormat(activity).format(d.getCreatedAt().getTime()) + " by " +
                                           d.getCreatedBy() + ".  " +
                                           "Modified on " + 
                                           DateFormat.getLongDateFormat(activity).format(d.getModifiedAt().getTime()) + " by " + 
                                           d.getModifiedBy()
                                           );

                    // Create second page notification
                    Notification secondPageNotification =
                            new NotificationCompat.Builder(activity)
                            .setStyle(secondPageStyle)
                            .build();
                    
                    replyNotificationBuilder.addPage(secondPageNotification);
                    
                    //replies.add("Open '" + d.getName() + "'");
                    
                    //Intent docIntent = new Intent(activity, PublicDispatcherActivity.class).setAction(IntentIntegrator.ACTION_DISPLAY_NODE);
                    //docIntent.putExtra(IntentIntegrator.EXTRA_NODE, (Parcelable)d);
                    //PendingIntent docPendingIntent = PendingIntent.getActivity(activity, 0, docIntent, 0);
                    
                    //notificationBuilder.addAction(mimeIconId, "Review documents", docPendingIntent);
                }
            }
            catch (Exception e) {}

            // Create the notification action for 'Action task'
            RemoteInput remoteInput = new RemoteInput.Builder(IntentIntegrator.EXTRA_VOICE_REPLY)
            .setLabel("Action task")
            .setChoices(replies.toArray(new String[replies.size()] ) )
            .build();
            
            Intent replyIntent = new Intent(activity, PublicDispatcherActivity.class).setAction(IntentIntegrator.ACTION_DISPLAY_TASK_ATTS);
            replyIntent.putExtra(IntentIntegrator.EXTRA_TASK, task);
            replyIntent.putExtra(IntentIntegrator.EXTRA_PROCESS, process);
            PendingIntent replyPendingIntent = PendingIntent.getActivity(activity, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Action replyAction = new Action.Builder(taskPriorityIconId, "Action task", replyPendingIntent).addRemoteInput(remoteInput).build();
        
            
            // Create wearable notification and add action
            Notification replyNotification = replyNotificationBuilder.addAction(replyAction).build();
                    //.setGroup("ALFRESCO_TASKS")
            
            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
       
            // Build the notification and issues it with notification manager.
            notificationManager.notify(notificationId++, replyNotification);  
            
            
            //Create summary notification for phone itself.
            /*
            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity)
                    .setSmallIcon(smallIconId)
                    .setLargeIcon(largeIcon);

            // Use the same group key and pass this builder to InboxStyle notification
            WearableNotifications.Builder wearableBuilder = new WearableNotifications
                    .Builder(builder)
                    .setGroup("ALFRESCO_TASKS", WearableNotifications.GROUP_ORDER_SUMMARY);

            // Build the final notification to show on the handset
            Notification summaryNotification = new NotificationCompat.InboxStyle(
                    wearableBuilder.getCompatBuilder())
                    .addLine("Alex Faaborg   Check this out")
                    .addLine("Jeff Chang   Launch Party")
                    .setBigContentTitle("2 new messages")
                    .setSummaryText("johndoe@gmail.com")
                    .build();

            notificationManager.notify((notificationId*2), summaryNotification);
            */
        }
    }
}
