package space.morphanone.webizen.events;

import com.denizenscript.denizen.Denizen;
//import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import space.morphanone.webizen.server.RequestWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class PostRequestScriptEvent extends BasicRequestScriptEvent {

    // <--[event]
    // @Events
    // post request
    //
    // @Regex ^on post request$
    //
    // @Triggers when the web server receives a POST request
    //
    // @Context
    // <context.address> Returns the IP address of the device that sent the request.
    // <context.request> Returns the path that was requested.
    // <context.query> Returns a ElementTag of the raw query included with the request.
    // <context.query_map> Returns a map of the query.
    // <context.user_info> Returns info about the authenticated user sending the request, if any.
    // <context.upload_name> returns the name of the file posted.
    // <context.upload_size_mb> returns the size of the upload in MegaBytes (where 1 MegaByte = 1 000 000 Bytes).
    // <context.headers> returns a MapTag of the headers of the request.
    //
    // @Determine
    // ElementTag to set the content of the response directly
    // "FILE:" + ElementTag to set the file for the response via a file path
    // "PARSED_FILE:" + ElementTag to set the parsed file for the response via a file path, this will parse any denizen tags inside the file
    // "CODE:" + ElementTag to set the HTTP status code of the response (e.g. 200)
    // "TYPE:" + ElementTag to set the MIME (multi purpose mail extension) of the response (e.g. text/html)
    // "SAVE_UPLOAD:" + ElementTag to save the upload to a file.
    //
    // @Plugin Webizen
    // -->
    public PostRequestScriptEvent() {
        instance = this;
    }

    public static PostRequestScriptEvent instance;
    public ElementTag saveUpload;
    public byte[] requestBody;
    public ElementTag fileName;
    public RequestWrapper request;
    public ElementTag entire_body;
    public ElementTag body;

    @Override
    public String getRequestType() {
        return "Post";
    }

    @Override
    public void fire(HttpExchange httpExchange) {
        try {
            this.request = new RequestWrapper(httpExchange);
          //this.requestBody = request.getFile();
            this.fileName = new ElementTag(request.getFileName());
            this.body = new ElementTag(request.getBody());
            this.entire_body = new ElementTag(request.getEntireRequest());
        } catch (Exception e) {
            Debug.echoError(e);
        }
        this.saveUpload = null;

        super.fire(httpExchange);

        if (this.saveUpload != null) {
            try {
                File file = new File(Denizen.getInstance().getDataFolder(), saveUpload.asString());
                if (!Utilities.canWriteToFile(file)) {
                    Debug.echoError("Save failed: cannot save there!");
                    return;
                }
                file.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(this.requestBody);
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String lower = CoreUtilities.toLowerCase(determinationObj.toString());
        if (lower.startsWith("save_upload:")) {
            saveUpload = new ElementTag(determinationObj.toString().substring(12));
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("upload_name")) {
            return fileName;
        }
        else if (name.equals("upload_size_mb")) {
            return new ElementTag(requestBody.length/(1000*1000));
        }
        else if (name.equals("address")) {
            return new ElementTag(httpExchange.getRemoteAddress().getAddress().getHostAddress());
        }
        else if (name.equals("request")) {
            return new ElementTag(httpExchange.getHttpContext().getPath());
        }
        else if (name.equals("query")) {
            return this.entire_body;
        }
        else if (name.equals("query_map") ) {
	        MapTag mappedValues = new MapTag();
	        try {
		        String query = this.request.getEntireRequest();
		        if (query != null) {
		            for (String value : CoreUtilities.split(query, '&')) {
		                List<String> split = CoreUtilities.split(value, '=', 2);
	                    String split_key = java.net.URLDecoder.decode(split.get(0), "UTF-8");
	                    String split_value = java.net.URLDecoder.decode(split.get(1), "UTF-8");
	                    mappedValues.putObject(split_key, new ElementTag(split_value));
		            }
		        }
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
	        return mappedValues;
        }
        else if (name.equals("headers")) {
            MapTag map = new MapTag();
            Headers headers = httpExchange.getRequestHeaders();
            for (String key : headers.keySet()) {
                map.putObject(key, new ElementTag(headers.get(key).get(0)));
            }
            return map;
        }
        return super.getContext(name);
    }
}
