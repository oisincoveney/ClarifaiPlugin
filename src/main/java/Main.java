public class Main
{
    public static void main ( String[] args )
    {

        ClarifaiTagger tagger = new ClarifaiTagger(
                "/Users/oisincoveney/Documents/Github/ClarifaiPlugin/build/resources/test/deer.jpeg",
                "/Users/oisincoveney/Documents/Github/ClarifaiPlugin/build/resources/test/soccer.jpeg",
                "http://lorempixel.com/400/200/");
        tagger.add("http://lorempixel.com/500/500");
        tagger.calculate();
        System.out.println(tagger + "\n---\ndone\n---\n");
        System.out.println(tagger.getTags("http://lorempixel.com/500/500"));
    }
}
