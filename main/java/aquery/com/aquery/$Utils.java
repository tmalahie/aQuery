package aquery.com.aquery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * A class containing all useful methods that are not related to any AQuery element
 */
@SuppressWarnings("unused")
public class $Utils {
    protected Activity ctx; // The activity associated

    public $Utils(Activity ctx) {
        setActivity(ctx);
    }

    /**
     * Sets the reference to the activity associated
     */
    protected void setActivity(Activity ctx) {
        this.ctx = ctx;
        if ((ctx != null) && tag.equals(""))
            tag = ctx.getClass().getSimpleName();
    }

    /**
     * A class to handle HTTP queries
     */
    public static class AjaxQuery {
        private Activity ctx;

        public AjaxQuery(Activity ctx) {
            this.ctx = ctx;
        }

        /**
         * A class to handle post data, that is a link between an URL parameter attribute and its associated value
         */
        public static class PostData {
            public final String attr;
            public final String value;

            /**
             * Constructor of PostData
             * @param attr
             * The URL parameter attribute
             * @param value
             * The URL parameter value
             */
            public PostData(String attr, Object value) {
                this.attr = attr;
                if (value == null)
                    this.value = null;
                else
                    this.value = value.toString();
            }
        }

        /**
         * Converts a PostData array to its String URL parameter
         * @param params
         * The parameters. For example {new PostData("attr1","value1"),new PostData("attr2","value2")}
         * @return
         * The value. For example "attr1=value1&attr2=value2"
         */
        public static String arrayToStringParams(PostData[] params) {
            String res = "";
            boolean oneValue = false;
            for (PostData param : params) {
                if (param.value != null) {
                    if (oneValue)
                        res += '&';
                    else
                        oneValue = true;
                    res += param.attr;
                    res += '=';
                    try {
                        res += URLEncoder.encode(param.value, "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
            return res;
        }

        /**
         * An exception thrown when an HTTP response error occurred
         * Allows to retreve the error code (404, 500, etc)
         */
        public static class FailureResponseException extends Exception {
            private static final long serialVersionUID = 1L;
            private int code;

            public FailureResponseException(int errorCode) {
                this(errorCode,String.valueOf(errorCode));
            }
            public FailureResponseException(int errorCode, String msg) {
                super(msg);
                code = errorCode;
            }

            /**
             * Returns the error code (404, 500, etc)
             */
            public int getErrorCode() {
                return code;
            }
        }

        /**
         * An interface to execute function when the ajax function has completed
         */
        public interface NetworkListener {
            /**
             * Function called when the ajax executed successfully
             * @param res
             * The response text
             */
            void done(String res);

            /**
             * Function called when an error occurred during the request
             * @param e
             * The exception responsible for the error
             */
            void fail(Exception e);

            /**
             * Function called always when an ajax function ha completed
             * This function is called after done of fail function
             *
             * @param success
             * true if the request executed successfully, false otherwise
             */
            void always(boolean success);
        }
        /**
         * An NetworkListener interface specialized for JSON responses
         */
        public interface JSONNetworkListener {
            /**
             * Function called when the ajax executed successfully.
             * You can throw a JSONException in the signature of the function
             * so that you can process the JSON result without putting heavy try/catch blocks
             * If the exception is actually thrown, the fail() function will be called
             * @param res
             * The response, parsed as JSON.
             * Is the parse fails, it will run fail() function with a JSONParseException as argument
             */
            void done(JSONObject res) throws JSONException;
            /**
             * Function called when an error occurred during the request
             * @param e
             * The exception responsible for the error
             */
            void fail(Exception e);
            /**
             * Function called always when an ajax function ha completed
             * This function is called after done of fail function
             *
             * @param success
             * true if the request executed successfully, false otherwise
             */
            void always(boolean success);
        }

        /**
         * A NetworkListener that simply does nothing on complete
         */
        private static final NetworkListener DONOTHING = new NetworkListener() {
            @Override
            public void done(String data) {
            }

            @Override
            public void fail(Exception e) {
            }

            @Override
            public void always(boolean success) {
            }
        };
        private String url = "";
        private NetworkListener onload = DONOTHING;

        /**
         * Treats the HTTP result on the UI thread
         * @param data
         * The data if it exists, null otherwise
         * @param err
         * The error if one occurred, null otherwise
         */
        private void treatResult(final String data, final Exception err) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean success = (data!=null);
                    if (success)
                        onload.done(data);
                    else
                        onload.fail(err);
                    onload.always(success);
                }
            });
        }

        /**
         * Load data from the server using a HTTP GET request
         * @param url
         * The url of the page to load
         */
        public AjaxQuery get(String url) {
            this.url = url;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String data = null;
                    Exception err = null;
                    try {
                        URL obj = new URL(AjaxQuery.this.url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        // optional default is GET
                        con.setRequestMethod("GET");

                        int responseCode = con.getResponseCode();
                        if (responseCode != HttpURLConnection.HTTP_OK)
                            throw new FailureResponseException(responseCode);

                        data = getStringFromInputStream(con.getInputStream());
                        con.disconnect();
                    }
                    catch (Exception e) {
                        err = e;
                    }
                    treatResult(data,err);
                }
            }).start();
            return this;
        }
        public AjaxQuery get(String url, NetworkListener manager) {
            this.onload = manager;
            return get(url);
        }
        public AjaxQuery get(String url, String params, NetworkListener manager) {
            return get(url + '?' + params, manager);
        }
        public AjaxQuery get(String url, PostData[] params, NetworkListener manager) {
            return get(url, arrayToStringParams(params), manager);
        }
        public AjaxQuery get(String url, JSONNetworkListener manager) {
            return get(url, toNetworkListener(manager));
        }
        public AjaxQuery get(String url, String params, JSONNetworkListener manager) {
            return get(url + '?' + params, manager);
        }
        public AjaxQuery get(String url, PostData[] params, JSONNetworkListener manager) {
            return get(url, arrayToStringParams(params), manager);
        }
        public AjaxQuery post(String url) {
            return post(url, new PostData[0]);
        }
        public AjaxQuery post(String url, final PostData[] params) {
            return post(url, arrayToStringParams(params));
        }
        public AjaxQuery post(String url, final String params) {
            this.url = url;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Exception err = null;
                    String data = null;
                    try {URL obj = new URL(AjaxQuery.this.url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        con.setRequestMethod("POST");

                        con.setDoOutput(true);
                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(params);
                        wr.flush();
                        wr.close();

                        int responseCode = con.getResponseCode();
                        if (responseCode != HttpURLConnection.HTTP_OK)
                            throw new FailureResponseException(responseCode);

                        data = getStringFromInputStream(con.getInputStream());
                        con.disconnect();
                    }
                    catch (Exception e) {
                        err = e;
                    }
                    treatResult(data,err);
                }
            }).start();
            return this;
        }
        public AjaxQuery post(String url, NetworkListener manager) {
            return post(url, new PostData[0], manager);
        }
        public AjaxQuery post(String url, String params, NetworkListener manager) {
            this.onload = manager;
            return post(url, params);
        }
        public AjaxQuery post(String url, PostData[] params, NetworkListener manager) {
            return post(url, arrayToStringParams(params), manager);
        }
        public AjaxQuery post(String url, String params, JSONNetworkListener manager) {
            return post(url, params, toNetworkListener(manager));
        }
        public AjaxQuery post(String url, PostData[] params, JSONNetworkListener manager) {
            return post(url, arrayToStringParams(params), manager);
        }
        public AjaxQuery finish(NetworkListener l) {
            onload = l;
            return this;
        }
        public AjaxQuery finish(JSONNetworkListener l) {
            return finish(toNetworkListener(l));
        }
        public AjaxQuery ajax(String url, String method, PostData[] params, NetworkListener manager) {
            if ("post".equals(method))
                return post(url, params, manager);
            else if ("get".equals(method))
                return get(url, params, manager);
            return this;
        }
        public AjaxQuery ajax(String url, String method, PostData[] params, JSONNetworkListener manager) {
            if ("post".equals(method))
                return post(url, params, manager);
            else if ("get".equals(method))
                return get(url, params, manager);
            return this;
        }
        public AjaxQuery ajax(String url, String method, NetworkListener manager) {
            return ajax(url, method, new PostData[0], manager);
        }
        public AjaxQuery ajax(String url, String method, JSONNetworkListener manager) {
            return ajax(url, method, new PostData[0], manager);
        }
        public AjaxQuery ajax(String url, String method) {
            return ajax(url, method, new PostData[0], DONOTHING);
        }
        public AjaxQuery ajax(String url) {
            return ajax(url, "get");
        }

        private NetworkListener toNetworkListener(final JSONNetworkListener manager) {
            return new NetworkListener() {
                @Override
                public void done(String res) {
                    JSONObject obj;
                    try {
                        obj = new JSONObject(res);
                    } catch (JSONException e) {
                        fail(new JSONParseException(res,e));
                        return;
                    }
                    try {
                        manager.done(obj);
                    } catch (JSONException e) {
                        fail(e);
                    }
                }

                @Override
                public void fail(Exception e) {
                    manager.fail(e);
                }

                @Override
                public void always(boolean success) {
                    manager.always(success);
                }
            };
        }
        private static String getStringFromInputStream(InputStream is) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();

            return response.toString();
        }
    }

    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url) {
        return new AjaxQuery(ctx).get(url);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).get(url, manager);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, String params, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).get(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, AjaxQuery.PostData[] params, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).get(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).get(url, manager);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, String params, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).get(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP GET request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery get(String url, AjaxQuery.PostData[] params, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).get(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url) {
        return new AjaxQuery(ctx).post(url);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, final AjaxQuery.PostData[] params) {
        return new AjaxQuery(ctx).post(url, params);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, final String params) {
        return new AjaxQuery(ctx).post(url, params);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).post(url, manager);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, String params, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).post(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, AjaxQuery.PostData[] params, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).post(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, String params, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).post(url, params, manager);
    }
    /**
     * Load data from the server using a HTTP POST request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery post(String url, AjaxQuery.PostData[] params, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).post(url, params, manager);
    }

    /**
     * Perform an asynchronous HTTP (Ajax) request
     * @param url
     * The url of the page to load
     * @param method
     * The request method ("get" or "post" )
     * @param params
     * The url parameters
     * @param manager
     * The functions called when the request has completed
     */
    public AjaxQuery ajax(String url, String method, AjaxQuery.PostData[] params, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).ajax(url, method, params, manager);
    }
    /**
     * Perform an asynchronous HTTP (Ajax) request and automatically parse the result into a JSON format
     * @param url
     * The url of the page to load
     * @param method
     * The request method ("get" or "post" )
     * @param params
     * The url parameters
     * @param manager
     * The functions called when the request has completed
     */
    public AjaxQuery ajax(String url, String method, AjaxQuery.PostData[] params, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).ajax(url, method, params, manager);
    }
    /**
     * Perform an asynchronous HTTP (Ajax) request
     * @param url
     * The url of the page to load
     * @param method
     * The request method ("get" or "post" )
     * @param manager
     * The functions called when the request has completed
     */
    public AjaxQuery ajax(String url, String method, AjaxQuery.NetworkListener manager) {
        return new AjaxQuery(ctx).ajax(url, method, manager);
    }
    /**
     * Perform an asynchronous HTTP (Ajax) request and automatically parse the result into a JSON format
     * @param url
     * The url of the page to load
     * @param method
     * The request method ("get" or "post" )
     * @param manager
     * The functions called when the request has completed
     */
    public AjaxQuery ajax(String url, String method, AjaxQuery.JSONNetworkListener manager) {
        return new AjaxQuery(ctx).ajax(url, method, manager);
    }
    /**
     * Perform an asynchronous HTTP (Ajax) request
     * @param url
     * The url of the page to load
     * @param method
     * The request method ("get" or "post" )
     */
    public AjaxQuery ajax(String url, String method) {
        return new AjaxQuery(ctx).ajax(url, method);
    }
    /**
     * Perform an asynchronous HTTP (Ajax) request
     * @param url
     * The url of the page to load
     */
    public AjaxQuery ajax(String url) {
        return new AjaxQuery(ctx).ajax(url);
    }

    /**
     * Converts an array of post data to its String url representation
     * @param data
     * The post parameters. For example {new PostData("attr1","value1"),new PostData("attr2","value2")}
     * @return
     * The value. For example "attr1=value1&attr2=value2"
     */
    public String param(AjaxQuery.PostData[] data) {
        return AjaxQuery.arrayToStringParams(data);
    }

    /**
     * An exception thrown when a parsing failed.
     * Allows to access the text that failed to be parsed
     */
    public static class ParseException extends Exception {
        private String input;

        /**
         * Constructor of ParseException
         * @param input
         * The original text that failed to be parsed
         * @param e
         * The exception thrown
         */
        public ParseException(String input, Exception e) {
            super("Error while parsing \""+ input +"\": "+ e.getMessage());
            this.input = input;
        }

        /**
         * Returns the original text that failed to be parsed
         */
        public String getInput() {
            return input;
        }
    }

    /**
     * An exception thrown when a JSON parsing failed
     * Allows to access the text that failed to be parsed
     */
    public static class JSONParseException extends ParseException {
        /**
         * Constructor of JSONParseException
         *
         * @param input The original text that failed to be parsed
         * @param e
         * The exception thrown
         */
        public JSONParseException(String input, JSONException e) {
            super(input, e);
        }
    }

    /**
     * Creates a View from its specified class name
     * @param name
     * The View class name, for example "TextView"
     * @return
     * An AQuery object containing the View
     */
    public AQuery create(String name) {
        return create(ctx, name);
    }

    /**
     * Creates a View from its specified class
     * @param viewClass
     * The View class, for example TextView.class
     * @return
     * An AQuery object containing the View
     */
    @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
    public AQuery create(Class viewClass) {
        try {
            return new $Element(ctx, (View) viewClass.getConstructor(Context.class).newInstance(ctx));
        }
        catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
    /**
     * Creates a View from its specified class name
     * @param ctx
     * The activity in which create the view
     * @param name
     * The View class name, for example "TextView"
     * @return
     * An AQuery object containing the View
     */
    @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
    public static AQuery create(Activity ctx, String name) {
        try {
            return new $Element(ctx, (View) getViewClass(ctx,name).getConstructor(Context.class).newInstance(ctx));
        }
        catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Tries to find the view class based on the class name specified
     * @param ctx
     * The activity context
     * @param name
     * The class name. For example "TextView"
     * @return
     * The class of the View if it has been found. Throws an exception otherwise
     */
    private static Class getViewClass(Context ctx, String name) {
        try {
            try {
                return Class.forName(name);
            }
            catch (ClassNotFoundException e) {
                try {
                    return Class.forName("android.widget." + name);
                }
                catch (ClassNotFoundException e2) {
                    try {
                        return Class.forName("android.view." + name);
                    }
                    catch (ClassNotFoundException e3) {
                        try {
                            return Class.forName("android.webkit." + name);
                        }
                        catch (ClassNotFoundException e4) {
                            try {
                                return Class.forName("android.opengl." + name);
                            }
                            catch (ClassNotFoundException e5) {
                                try {
                                    return Class.forName(ctx.getClass().getPackage().getName() +'.'+ name);
                                }
                                catch (ClassNotFoundException e6) {
                                    try {
                                        return Class.forName(ctx.getPackageName() + '.' + name);
                                    }
                                    catch (ClassNotFoundException e7) {
                                        throw e;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class name \""+ name +"\"");
        }
    }

    /**
     * Checks if a view has the given child
     * @param parent
     * The parent
     * @param child
     * The child
     * @return
     * true if it has, false otherwise
     */
    public boolean hasChild(View parent, View child) {
        ViewGroup vParent;
        try {
            vParent = (ViewGroup) parent;
        }
        catch (ClassCastException e) {
            return false;
        }
        for (int i=0;i<vParent.getChildCount();i++) {
            View elt = vParent.getChildAt(i);
            if ((elt == child) || hasChild(elt, child))
                return true;
        }
        return false;
    }

    /**
     * Shows an informative dialog box with a "Ok" button
     * @param title
     * The title of the dialog box. May be null
     */
    public void inform(String title) {
        inform(title, (String)null);
    }
    /**
     * Shows an informative dialog box with a "Ok" button
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     */
    public void inform(String title, String message) {
        inform(title, message, null);
    }
    /**
     * Shows an informative dialog box with a "Ok" button
     * @param title
     * The title of the dialog box. May be null
     * @param onClose
     * The function to call when the user closes the dialog box. May be null
     */
    public void inform(String title, Runnable onClose) {
        inform(title, null, onClose);
    }
    /**
     * Shows an informative dialog box with a "Ok" button
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param onClose
     * The function to call when the user closes the dialog box. May be null
     */
    public void inform(String title, String message, Runnable onClose) {
        inform(title, message, onClose, onClose);
    }
    /**
     * Shows an informative dialog box with a "Ok" button
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param onValid
     * The function to call when the user clicks on "Ok". May be null
     * @param onCancel
     * The function to call when the user dismisses the dialog box (click on "return" button). May be null
     */
    public void inform(String title, String message, final Runnable onValid, final Runnable onCancel) {
        popup(title,message)
            .setPositiveButton(android.R.string.ok, (onValid==null) ? null:new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onValid.run();
                }
            }).setOnCancelListener((onCancel==null) ? null:new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    onCancel.run();
                }
            }).show();
    }

    /**
     * Show a dialog box to ask the user to confirm an action
     * @param title
     * The title of the dialog box. May be null
     * @param onConfirm
     * The function called when the user clicks on yes
     */
    public void confirm(String title, Runnable onConfirm) {
        confirm(title, (String) null, onConfirm);
    }
    /**
     * Show a dialog box to ask the user to confirm an action
     * @param title
     * The title of the dialog box. May be null
     * @param onConfirm
     * The function called when the user clicks on yes
     */
    public void confirm(String title, String message, Runnable onConfirm) {
        confirm(title, message, onConfirm, null);
    }
    /**
     * Show a dialog box to ask the user to confirm an action
     * @param title
     * The title of the dialog box
     * @param onConfirm
     * The function called when the user clicks on yes
     */
    public void confirm(String title, Runnable onConfirm, Runnable onDeny) {
        confirm(title, null, onConfirm, onDeny);
    }
    /**
     * Show a dialog box to ask the user to confirm an action
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param onConfirm
     * The function called when the user clicks on yes
     * @param onDeny
     * (Optional) The function called when the user clicks on no or dismisses the dialog box
     */
    public void confirm(String title, String message, Runnable onConfirm, Runnable onDeny) {
        confirm(title, message, onConfirm, onDeny, onDeny);
    }
    /**
     * Show a dialog box to ask the user to confirm an action
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param onConfirm
     * The function called when the user clicks on yes
     * @param onDeny
     * (Optional) The function called when the user clicks on no
     * @param onCancel
     * (Optional) The function called when the user dismisses the dialog box
     */
    public void confirm(String title, String message, final Runnable onConfirm, final Runnable onDeny, final Runnable onCancel) {
        popup(title,message)
            .setPositiveButton(android.R.string.yes, (onConfirm==null) ? null:new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onConfirm.run();
                }
            }).setNegativeButton(android.R.string.no, (onDeny==null) ? null:new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDeny.run();
                }
            }).setOnCancelListener((onCancel==null) ? null:new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    onCancel.run();
                }
            }).show();
    }

    /**
     * An interface to handle prompt() method
     */
    public interface PromptListener {
        /**
         * The function called when the user clicked on "OK"
         * @param value
         * The value entered by the user
         */
        void onSubmit(String value);

        /**
         * The function called when the user dismisses the dialog box
         */
        void onCancel();
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, PromptListener callback) {
        prompt(title, null, callback);
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param inputType
     * The type of content that the user can enter, such as InputType.TYPE_CLASS_NUMBER
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, int inputType, PromptListener callback) {
        prompt(title, null, inputType, callback);
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, String message, PromptListener callback) {
        prompt(title, message, null, callback);
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param inputType
     * The type of content that the user can enter, such as InputType.TYPE_CLASS_NUMBER
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, String message, int inputType, PromptListener callback) {
        prompt(title, message, null, inputType, callback);
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param defaut
     * The default value of the text entered by the user. May be null
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, String message, String defaut, final PromptListener callback) {
        prompt(title, message, defaut, InputType.TYPE_CLASS_TEXT, callback);
    }

    /**
     * Shows a dialog box to ask the user to enter a text value
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @param defaut
     * The default value of the text entered by the user. May be null
     * @param inputType
     * The type of content that the user can enter, such as InputType.TYPE_CLASS_NUMBER
     * @param callback
     * The function to call when the user submits the dialog box
     */
    public void prompt(String title, String message, String defaut, int inputType, final PromptListener callback) {
        final AQuery promptView = create(EditText.class)
                .lp("match_parent", "wrap_content")
                .prop("inputType", inputType)
                .prop("singleLine", true);
        if (defaut != null)
            promptView.val(defaut);

        popup(title, message)
            .setPositiveButton(android.R.string.ok, (callback == null) ? null : new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    callback.onSubmit(promptView.val());
                }
            })
            .setNegativeButton(android.R.string.cancel, (callback == null) ? null : new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    callback.onCancel();
                }
            })
            .setOnCancelListener((callback == null) ? null : new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    callback.onCancel();
                }
            })
            .setView(promptView.head())
            .show();

        promptView.select().showKeyboard();
    }

    /**
     * An interface for choose() function
     */
    public interface ChoiceListener {
        /**
         * Function called when the user chosed somethin
         * @param id
         * The id of the item selected
         * @param choice
         * The text value of the item selected
         */
        void onChose(int id, String choice);

        /**
         * Function called when the user cancelled his action
         */
        void onCancel();
    }

    /**
     * Shows a dialog box to ask the user to select an item within a list of choices
     * @param choices
     * An array containing all possible choices
     * @param callback
     * The function called when the user has made his choice
     */
    public void choose(String[] choices, ChoiceListener callback) {
        choose(null, choices, callback);
    }
    /**
     * Shows a dialog box to ask the user to select an item within a list of choices
     * @param title
     * The title of the dialog box. May be null
     * @param choices
     * An array containing all possible choices
     * @param callback
     * The function called when the user has made his choice
     */
    public void choose(String title, final String[] choices, final ChoiceListener callback) {
        popup(title).setItems(choices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onChose(which, choices[which]);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                callback.onCancel();
            }
        }).show();
    }

    /**
     * Creates a dialog box and returns it
     * @return
     * The dialog box created
     */
    public AlertDialog.Builder popup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            return new AlertDialog.Builder(new ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Light_Dialog));
        else
            return new AlertDialog.Builder(ctx);
    }
    /**
     * Creates a dialog box and returns it
     * @param title
     * The title of the dialog box. May be null
     * @return
     * The dialog box created
     */
    public AlertDialog.Builder popup(String title) {
        return popup().setTitle(title);
    }
    /**
     * Creates a dialog box and returns it
     * @param title
     * The title of the dialog box. May be null
     * @param message
     * The message to display. May be null
     * @return
     * The dialog box created
     */
    public AlertDialog.Builder popup(String title, String message) {
        return popup(title).setMessage(message);
    }

    private ProgressDialog pageLoading; // The currently shown progress dialog

    /**
     * Shows a dialog box telling the user that something is loading
     * @return
     * The progress dialog created
     */
    public ProgressDialog loading() {
        return loading(false);
    }
    /**
     * Shows a dialog box telling the user that something is loading
     * @param message
     * The message to display. Default is "Loading…"
     * @return
     * The progress dialog created
     */
    public ProgressDialog loading(String message) {
        return loading(message, false);
    }
    /**
     * Shows a dialog box telling the user that something is loading
     * @param preventBack
     * If set to true, the user won't be able to close the progress dialog himself
     * @return
     * The progress dialog created
     */
    public ProgressDialog loading(boolean preventBack) {
        return loading("Loading…", preventBack);
    }
    /**
     * Shows a dialog box telling the user that something is loading
     * @param message
     * The message to display. Default is "Loading…"
     * @param preventBack
     * If set to true, the user won't be able to close the progress dialog himself
     * @return
     * The progress dialog created
     */
    public ProgressDialog loading(String message, boolean preventBack) {
        if (pageLoading != null)
            pageLoading.dismiss();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            pageLoading = new ProgressDialog(new ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth));
        else
            pageLoading = new ProgressDialog(ctx);
        pageLoading.setMessage(message);
        pageLoading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pageLoading.setIndeterminate(true);
        if (preventBack) {
            pageLoading.setCancelable(false);
            pageLoading.setCanceledOnTouchOutside(false);
        }
        else {
            final ProgressDialog currentPageLoading = pageLoading;
            pageLoading.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    currentPageLoading.dismiss();
                    if (pageLoading == currentPageLoading)
                        pageLoading = null;
                }
            });
        }
        try {
            pageLoading.show();
        }
        catch (Exception e) {
            pageLoading = null;
        }
        return pageLoading;
    }

    /**
     * Checks if the given progress dialog has been closed. Useful to abort a task if the user cancelled it
     * @param loadingDialog
     * The progress dialog to check
     * @return
     * True if the progress dialog has been closed, false otherwise
     */
    public boolean aborted(ProgressDialog loadingDialog) {
        return (pageLoading != loadingDialog);
    }
    /**
     * Closes the currently shown progress dialog, if it exists
     */
    public void endload() {
        if (pageLoading != null) {
            pageLoading.dismiss();
            pageLoading = null;
        }
    }

    /**
     * Parses an XML view and wrap the result into an AQuery object
     * @param xml
     * The XML code to parse
     * @deprecated This code is really slow to execute. Prefer creating an XML file in your layout folder
     * and calling inflate(int) method instead
     */
    public AQuery inflate(String xml) {
        return new $Array(ctx, xml, (AQuery)null);
    }
    /**
     * Parses an XML layout and wrap the result into an AQuery object
     * @param layout
     * The ressoure id of the layout
     */
    public AQuery inflate(int layout) {
        return new $Element(ctx, LayoutInflater.from(ctx).inflate(layout, null));
    }
    /**
     * Returns the union of 2 sets, removing duplicates.
     * This function assumes that the first list has no duplicates
     */
    public <T> List<T> union(List<T> l1, List<T> l2) {
        return AQuery.union(l1, l2);
    }
    /**
     * Returns the intersection of 2 sets, removing duplicates.
     * This function assumes that the first list has no duplicates
     */
    public <T> List<T> intersection(List<T> l1, List<T> l2) {
        return AQuery.intersection(l1, l2);
    }
    public boolean equals(Object a, Object b) {
        return AQuery.equals(a, b);
    }
    /**
     * Returns the width resolution of the device, in px
     */
    public int width() {
        return size().widthPixels;
    }
    /**
     * Returns the height resolution of the device, in px
     */
    public int height() {
        return size().heightPixels;
    }

    /**
     * Returns the first element of the activity
     */
    public AQuery root() {
        return new $Document(ctx).children().first();
    }

    /**
     * Returns the elements that have the given ID
     */
    public AQuery id(int id) {
        return new $Document(ctx).find(id);
    }

    /**
     * Returns all the elements of the activity
     */
    public AQuery all() {
        return new $Document(ctx).descendants();
    }

    /**
     * Returns the resolution of the device, in px
     */
    private DisplayMetrics size() {
        Display display = ctx.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics;
    }

    /**
     * Converts a given unit to pixels
     * @param size
     * The size to convert
     * @param unit
     * The unit of the size given
     */
    public float px(float size, int unit) {
        return AQuery.toPX(ctx, size, unit);
    }
    /**
     * Converts a given unit to pixels. Give an integer for the result
     * @param size
     * The size to convert
     * @param unit
     * The unit of the size given. Use TypedValue to get the possible units
     */
    public int pxi(float size, int unit) {
        return Math.round(px(size, unit));
    }
    /**
     * Converts a given unit to pixels.
     * @param size
     * The size to convert
     * @param unit
     * The unit of the size given, in a String form. For example : "dp"
     */
    public float px(float size, String unit) {
        return AQuery.toPX(ctx, size, unit);
    }
    /**
     * Converts a given unit to pixels. Give an integer for the result
     * @param size
     * The size to convert
     * @param unit
     * The unit of the size given, in a String form. For example : "dp"
     */
    public int pxi(float size, String unit) {
        return Math.round(px(size, unit));
    }
    public float sp(float px) {
        return px/metrics().scaledDensity;
    }
    public int spi(float px) {
        return Math.round(sp(px));
    }
    public float dp(float px) {
        return px/metrics().density;
    }
    public int dpi(float px) {
        return Math.round(dp(px));
    }

    /**
     * Converts a given amount of pixels to the desired unit
     * @param px
     * The number of pixels
     * @param unit
     * The unit of the result. Use TypedValue to get the possible units
     */
    public float convert(float px, int unit) {
        return px/AQuery.toPX(ctx, 1, unit);
    }
    /**
     * Converts a given amount of pixels to the desired unit
     * @param px
     * The number of pixels
     * @param unitName
     * The unit of the result, in a String form. For example : "dp"
     */
    public float convert(float px, String unitName) {
        return convert(px, unitID(unitName));
    }

    /**
     * Converts a given unit into another given unit
     * @param size
     * The size to convert
     * @param inputUnit
     * The unit of the size. Use TypedValue to get the possible units
     * @param outputUnit
     * The unit of the response. Use TypedValue to get the possible units
     */
    public float convert(float size, int inputUnit, int outputUnit) {
        return convert(px(size, inputUnit), outputUnit);
    }
    /**
     * Converts a given unit into another given unit
     * @param size
     * The size to convert
     * @param inputUnit
     * The unit of the size, in a String form. For example : "dp"
     * @param outputUnit
     * The unit of the response, in a String form. For example : "sp"
     */
    public float convert(float size, String inputUnit, String outputUnit) {
        return convert(px(size, inputUnit), outputUnit);
    }

    /**
     * Returns the resource display metrics
     */
    private DisplayMetrics metrics() {
        return ctx.getResources().getDisplayMetrics();
    }

    /**
     * Converts a unit name (for example : "dp")  to its associated TypedValue (For example : TypedValue.COMPLEX_UNIT_DIP)
     * @param unitName
     * The unit name
     */
    public static int unitID(String unitName) {
        if ("".equals(unitName) || "px".equals(unitName))
            return TypedValue.COMPLEX_UNIT_PX;
        if ("dp".equals(unitName) || "dip".equals(unitName))
            return TypedValue.COMPLEX_UNIT_DIP;
        if ("sp".equals(unitName))
            return TypedValue.COMPLEX_UNIT_SP;
        if ("in".equals(unitName))
            return TypedValue.COMPLEX_UNIT_IN;
        if ("mm".equals(unitName))
            return TypedValue.COMPLEX_UNIT_MM;
        if ("pt".equals(unitName))
            return TypedValue.COMPLEX_UNIT_PT;
        throw new IllegalArgumentException("Unknown unit \""+ unitName +"\"");
    }

    /**
     * Sets the layout content view of the activity
     * @param layout
     * The resource ID of the layout to show
     */
    public void content(int layout) {
        ctx.setContentView(layout);
    }
    /**
     * Sets the layout content view of the activity
     * @param layout
     * The name of the layout to show
     */
    public void content(String layout) {
        ctx.setContentView(AQuery.getIdentifier(ctx,layout));
    }
    /**
     * Sets the content view of the activity
     * @param v
     * The view to show
     */
    public void content(View v) {
        ctx.setContentView(v);
    }
    /**
     * Sets the content view of the activity
     * @param q
     * The element to show
     */
    public void content(AQuery q) {
        content(q.head());
    }

    /**
     * Starts a new activity
     * @param activity
     * The activity class
     */
    public void open(Class<?> activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(ctx, activity);
        ctx.startActivity(intent);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class
     * @param flags
     * The flags to control how the intent is handled
     */
    public void open(Class<?> activity, int flags) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(ctx, activity);
        intent.setFlags(flags);
        ctx.startActivity(intent);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class
     * @param extras
     * The extra parameters to pass to the activity
     */
    public void open(Class<?> activity, Bundle extras) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(ctx, activity);
        intent.putExtras(extras);
        ctx.startActivity(intent);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class
     * @param flags
     * The flags to control how the intent is handled
     * @param extras
     * The extra parameters to pass to the activity
     */
    public void open(Class<?> activity, int flags, Bundle extras) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(ctx, activity);
        intent.setFlags(flags);
        intent.putExtras(extras);
        ctx.startActivity(intent);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class name
     */
    public void open(String activity) {
        open(getClass(activity));
    }

    /**
     * Starts a new activity
     * @param activity
     * The activity class name
     * @param flags
     * The flags to control how the intent is handled
     */
    public void open(String activity, int flags) {
        open(getClass(activity), flags);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class name
     * @param extras
     * The extra parameters to pass to the activity
     */
    public void open(String activity, Bundle extras) {
        open(getClass(activity), extras);
    }
    /**
     * Starts a new activity
     * @param activity
     * The activity class name
     * @param flags
     * The flags to control how the intent is handled
     * @param extras
     * The extra parameters to pass to the activity
     */
    public void open(String activity, int flags, Bundle extras) {
        open(getClass(activity), flags, extras);
    }

    /**
     * Shows the keayboard if it's hidden and hides it if it's shown
     */
    public void toggleKeyboard() {
        ((InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, 0);
    }

    /**
     * Returns the class based on the activity namz
     * @param activityName
     * The name of the activity, like "MainActivity"
     */
    private Class<?> getClass(String activityName) {
        try {
            return Class.forName(ctx.getClass().getPackage().getName() +'.'+ activityName);
        }
        catch (ClassNotFoundException e) {
            try {
                return Class.forName(ctx.getPackageName() +'.'+ activityName);
            }
            catch (ClassNotFoundException e1) {
                try {
                    return Class.forName(activityName);
                }
                catch (ClassNotFoundException e2) {
                    throw new IllegalArgumentException("Unable to find activity \""+ activityName +"\"");
                }
            }
        }
    }

    /**
     * Returns the String associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public String string(int resourceID) {
        return ctx.getResources().getString(resourceID);
    }
    /**
     * Returns the Drawable associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    @SuppressWarnings("deprecation")
    public Drawable drawable(int resourceID) {
        return ctx.getResources().getDrawable(resourceID);
    }
    /**
     * Returns the integer associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public int integer(int resourceID) {
        return ctx.getResources().getInteger(resourceID);
    }
    /**
     * Returns the dimension associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public float dimen(int resourceID) {
        return ctx.getResources().getDimension(resourceID);
    }
    /**
     * Returns the boolean associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public Object bool(int resourceID) {
        return ctx.getResources().getBoolean(resourceID);
    }
    /**
     * Returns the String Array associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public String[] stringArray(int resourceID) {
        return ctx.getResources().getStringArray(resourceID);
    }
    /**
     * Returns the color associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    @SuppressWarnings("deprecation")
    public int color(int resourceID) {
        return ctx.getResources().getColor(resourceID);
    }
    /**
     * Returns the animation associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public XmlResourceParser anim(int resourceID) {
        return ctx.getResources().getAnimation(resourceID);
    }
    /**
     * Returns the layout associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public XmlResourceParser layout(int resourceID) {
        return ctx.getResources().getLayout(resourceID);
    }
    /**
     * Returns the movie associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public Object movie(int resourceID) {
        return ctx.getResources().getMovie(resourceID);
    }
    /**
     * Returns the xml associated to the resource identifier
     * @param resourceID
     * The resource identifier
     */
    public Object xml(int resourceID) {
        return ctx.getResources().getXml(resourceID);
    }

    /**
     * Displays a Toast information message to the user
     * @param text
     * The text to display
     * @return
     * The Toast Object created
     */
    public Toast toast(String text) {
        return toast(text, Toast.LENGTH_SHORT);
    }
    /**
     * Displays a Toast information message to the user
     * @param text
     * The text to display
     * @param duration
     * The duration, LENGTH_SHORT or LENGTH_LONG. Default is LENGTH_SHORT
     * @return
     * The Toast Object created
     */
    public Toast toast(String text, int duration) {
        Toast res = Toast.makeText(ctx, text, duration);
        res.show();
        return res;
    }
    /**
     * Displays a Toast information message to the user
     * @param resource
     * The text resource to display
     * @return
     * The Toast Object created
     */
    public Toast toast(int resource) {
        return toast(resource, Toast.LENGTH_SHORT);
    }
    /**
     * Displays a Toast information message to the user
     * @param resource
     * The text resource to display
     * @param duration
     * The duration, LENGTH_SHORT or LENGTH_LONG. Default is LENGTH_SHORT
     * @return
     * The Toast Object created
     */
    public Toast toast(int resource, int duration) {
        Toast res = Toast.makeText(ctx, resource, duration);
        res.show();
        return res;
    }

    private String tag = ""; // The tag used for log messages
    /**
     * Sets the default tag identifier used for log messages
     */
    public void tag(String tag) {
        this.tag = tag;
    }

    /**
     * Sends a verbose log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    public int verbose(Object message) {
        return verbose(tag,message);
    }

    /**
     * Sends a verbose log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    public int verbose(String tag, Object message) {
        return Log.v(tag, String.valueOf(message));
    }
    /**
     * Sends a debug log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    public int debug(Object message) {
        return debug(tag,message);
    }
    /**
     * Sends a debug log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    public int debug(String tag, Object message) {
        return Log.d(tag, String.valueOf(message));
    }
    /**
     * Sends an information log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    public int info(Object message) {
        return info(tag,message);
    }
    /**
     * Sends an information log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    public int info(String tag, Object message) {
        return Log.d(tag, String.valueOf(message));
    }
    /**
     * Sends a warning log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    public int warning(Object message) {
        return warning(tag,message);
    }
    /**
     * Sends a warning log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    public int warning(String tag, Object message) {
        return Log.w(tag, String.valueOf(message));
    }
    /**
     * Sends an error log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    public int error(Object message) {
        return error(tag,message);
    }
    /**
     * Sends an error log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    public int error(String tag, Object message) {
        return Log.e(tag, String.valueOf(message));
    }
    /**
     * Sends a failure log message.
     * By default, the log tag identifier for the log is the Activity name. Call tag to change it
     * @param message
     * The message to log
     * @return
     * The number of bytes written
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public int failure(Object message) {
        return failure(tag,message);
    }
    /**
     * Sends a failure log message
     * @param tag
     * The tag used to identify the log message
     * @param message
     * The message to display
     * @return
     * The number of bytes written
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public int failure(String tag, Object message) {
        return Log.wtf(tag, String.valueOf(message));
    }
}
