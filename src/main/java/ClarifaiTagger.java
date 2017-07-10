import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.request.model.PredictRequest;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by oisincoveney on 7/6/17.
 */
public class ClarifaiTagger
{

    // TODO : Convert to something that Nuxeo Platform can use
    @SuppressWarnings ( "FieldCanBeLocal" )
    private String APP_KEY_PATH = "/** PATH TO keys.txt GOES HERE";

    //API authorization keys
    private String API_KEY;       // -> App ID with OAuth2
    private String API_SECRET;    // -> App secret with OAuth2, null if API Keys

    //A PredictRequest based off ClarifaiClient
    private PredictRequest <Concept> client;

    //LinkedHashMap for storing the string of the image path, boolean to hold whether the image is online or local
    private LinkedHashMap <String, Boolean> imagePaths;

    //LinkedHashMap for storing the results of finding tags: String for image path, HashSet containing the image tags
    private LinkedHashMap <String, HashSet <String>> results;

    //Regular expression for determining whether a given link is a URL or not
    private Pattern REGEX = Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$");


    /**
     * initAppKeys() -> This method is called from the constructor to initialize the keys needed to access the
     * Clarifai REST API. The App ID and App Secret keys should be contained within a file called "keys.txt".
     *
     * The method will then initialize the API_KEY and API_SECRET OAuth2 keys as it reads the keys from the file
     * using a BufferedReader.
     *
     * Clarifai will be replacing its OAuth2 authentication system with API keys, and this method must be changed
     * to handle the new system. For now, the method will throw an exception if only one key is provided in the file.
     * In the future, the number of keys in the file should determine which authentication system to use.
     */
    private void initAppKeys ()
    {
        //Checks if App Key File exists

        if ( !Files.exists(Paths.get(APP_KEY_PATH)) )
        {
            throw new IllegalArgumentException("The file that contains API access keys cannot be found.");
        }

        //Gets the app keys. If there is an error with the file, an IOException will be thrown
        try
        {
            BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(APP_KEY_PATH));
            API_KEY = bufferedReader.readLine();
            API_SECRET = bufferedReader.readLine();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }


        // Checks whether the user's authentication implements OAuth2 or the API Keys
        if ( API_SECRET == null )
        {
            // TODO : Change to appropriate lines when Java API Keys are released for Clarifai

            // client = new ClarifaiBuilder(API_KEY).buildSync().getDefaultModels().generalModel().predict();
            throw new IllegalArgumentException();
        }
        else
        {
            client = new ClarifaiBuilder(API_KEY, API_SECRET).buildSync().getDefaultModels().generalModel().predict();
        }

    }

    /**
     * Default Constructor
     *
     * Calls initAppKeys() and initializes two empty LinkedHashMaps
     */
    ClarifaiTagger ()
    {
        //Initializes the app keys for accessing the Clarifai API
        initAppKeys();

        //Initialize the hash maps
        imagePaths = new LinkedHashMap <>();
        results = new LinkedHashMap <>();

    }

    /**
     * Overloaded constructor with variable String parameters
     *
     * Calls the default constructor, then adds the list of String objects to the imagePaths map
     */
    ClarifaiTagger ( String... imagePaths )
    {
        this();
        add(imagePaths);
    }

    /**
     * Adds the set of image paths and URLs to the imagePaths LinkedHashMap by determining whether
     * the string given indicates a local or online image, and calls the addOnlineImage() and
     * addLocalImage() depending on the result. If the string cannot be determined to be either a file path
     * or a URL, the method will return false, but will continue to evaluate the other strings until complete.
     *
     * @param image -> a set of strings containing URL or image paths of images to send to Clarifai API
     *
     * @return a boolean describing the final status of the operation -> true if the String given was recognized as a
     * URL or image path, false if not.
     */
    void add ( String... image )
    {
        boolean allImagesSuccessful = true;

        for ( String im : image )
        {
            if ( Files.exists(Paths.get(im)) )
                addLocalImage(im);
            else if ( REGEX.matcher(im).find() )
                addOnlineImage(im);
            else
                allImagesSuccessful = false;
        }

    }

    /**
     * Adds a string to the imagePaths LinkedHashMap, adding the string with a mapping to TRUE,
     * indicating that the image is online
     *
     * @param url -> a string that contains a url to an online image
     */
    private void addOnlineImage ( String url )
    {
        imagePaths.put(url, true);
    }

    /**
     * Adds a string to the imagePaths LinkedHashMap, adding the string with a mapping to FALSE,
     * indicating that the image is on the file system
     *
     * @param path -> a string that contains a path to a local image
     */
    private void addLocalImage ( String path )
    {
        imagePaths.put(path, false);
    }

    void calculate ()
    {

        //The collection that will be sent to Clarifai
        ArrayList <ClarifaiInput> inputs = new ArrayList <>();


        // Populates the input list from the list of image path strings
        imagePaths.forEach(( String str, Boolean bool ) ->
                           {

                               if ( bool )
                                   inputs.add(ClarifaiInput.forImage(str));
                               else
                                   inputs.add(ClarifaiInput.forImage(new File(str)));
                           });

        //Retrieve the results
        List <ClarifaiOutput <Concept>> outputs = client.withInputs(inputs).executeSync().get();


        Iterator <String>                   pathsIterator  = imagePaths.keySet().iterator();
        Iterator <ClarifaiOutput <Concept>> outputIterator = outputs.iterator();

        while ( pathsIterator.hasNext() && outputIterator.hasNext() )
        {
            results.put(pathsIterator.next(), tagListToSet(outputIterator.next().data()));
        }

    }

    /**
     * tagListToSet ( List<Concept> )
     *
     * Converts a list of Clarifai Concept objects into a HashSet containing only the tags
     * contained within the Concept object
     *
     * @param conceptList -> a list of Concept objects returned by the Clarifai Client after sending images to the API
     *
     * @return a HashSet containing the tags of the respective image's Concept list
     */
    private HashSet <String> tagListToSet ( List <Concept> conceptList )
    {
        HashSet <String> tags = new HashSet <>(conceptList.size());
        for ( Concept concept : conceptList )
        {
            tags.add(concept.name());
        }
        return tags;
    }


    /**
     * @return a LinkedHashMap containing pairs of image files and their respective tags from Clarifai
     */
    public LinkedHashMap <String, HashSet <String>> getResults ()
    {
        if ( results == null )
            calculate();

        return results;
    }

    /**
     * Returns the tags for a specific image path. Based on the behavior of the LinkedHashMap's get() method
     *
     * @param imagePath -> the full path or URL of the image
     * @return a HashSet of strings containing the tags for the specific image
     */
    public HashSet<String> getTags(String imagePath)
    {
        return results.get(imagePath);
    }


    /**
     * toString() -> returns a string representation of the results array
     */
    public String toString ()
    {
        StringBuilder s = new StringBuilder();
        results.forEach(( str, set ) ->
                        {
                            s.append((imagePaths.get(str) ? "URL  " : "Local") + "     |   ");
                            s.append("\t" + str);
                            s.append("\n" + set + "\n");
                        });
        return s.toString();
    }


}
