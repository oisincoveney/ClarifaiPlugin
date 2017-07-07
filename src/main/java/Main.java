public class Main
{
    public static void main ( String[] args )
    {
        ClarifaiTagger tagger = new ClarifaiTagger();
        tagger.add("http://lorempixel.com/400/200/");
        tagger.calculate();
        System.out.println(tagger);

    }
}
