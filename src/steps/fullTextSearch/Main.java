package steps.fullTextSearch;

import org.w3c.dom.*;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileReader;
import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

public class Main {

    public static class Doc {
        public String title;
        public String text;
        public String url;
        public int ID;

        public void showDoc() {
            System.out.println("Found in № "+ID) ;
            System.out.println("title: "+ title);
            System.out.println("url: "+ url);
            System.out.println("abstract: "+ text+"\n");
        }
    };

    public static String[] tokensSearch;
    public static Map<String, Set<Integer>> index = new HashMap<String, Set<Integer>>();
    public static Map<Integer, Doc> documents = new HashMap<Integer, Doc>();

    public static String[] tokenize(String string){
        return string.split(" ");
    }

    public static String removeMark(String string){
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isSpaceChar(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }


    public static String[] stopWordFilter(String[] wordsList){
        String[] stopWord = { "a", "and", "be", "have", "i", "in", "of", "that", "the", "to", ""};
        Set<String> setWordsList = new HashSet<String>(Arrays.asList(wordsList));
        Set<String> setStopWord = new HashSet<String>(Arrays.asList(stopWord));

        setWordsList.removeAll(setStopWord);
        setWordsList.removeAll(setStopWord);
        return setWordsList.toArray(new String[0]);
    }

    public static String[] analyze(String text){
        text = removeMark(text);
        text=text.toLowerCase();
        String[] result = tokenize(text);
        result = stopWordFilter(result);
        return result;
    }

    public static String[] addIndex(Doc doc){
        String[] tokens = analyze(doc.text);
        for (String token: tokens){
            Set<Integer> indexForToken = index.get(token);
            if(indexForToken == null)
                indexForToken = new HashSet<Integer>();
            indexForToken.add(doc.ID);
            index.put(token, indexForToken);
        }
        return tokens;
    }


    public static void addDoc(Doc doc){
        documents.put(doc.ID, doc);
    }


    public static void search(){
        System.out.println("Search... ");
        long startTime = System.currentTimeMillis();
        Set<Integer> tokensHashSet = new HashSet<>();
        for (String token: tokensSearch){
            Set<Integer> indexForToken = index.get(token);
            if(tokensHashSet.size() > 0) indexForToken.retainAll(tokensHashSet);
            tokensHashSet = indexForToken;
        }

        long endTime = System.currentTimeMillis();
        for(Integer docID: tokensHashSet){
            documents.get(docID).showDoc();
        }
        System.out.println("Total found " + tokensHashSet.size());
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
    }


    public static void enterSearchString(){
        System.out.println("Enter text for search or empty string to exit: ");
        Scanner sc = new Scanner(System.in);
        String search_string = sc.nextLine();
        tokensSearch = analyze(search_string);
        if (tokensSearch.length==0) {
            System.out.println("Exit, empty string");
            System.exit(0);
        }
    }


    public static void main(String[] args) throws FileNotFoundException, XMLStreamException, JAXBException {

        if (args.length>0){
            tokensSearch = analyze(Arrays.toString(args));
        }else {
            enterSearchString();
        }

        System.out.println("Indexing and Search... ");
        System.out.println("Tokens Search: "+ Arrays.toString(tokensSearch));

//        asdfsadf
        int count = 0;
        int countFound = 0;
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(new FileReader("enwiki-latest-abstract1.xml"));
        xsr.nextTag();
        xsr.next();

        final JAXBContext jaxbContext = JAXBContext.newInstance();
        final Unmarshaller unm = jaxbContext.createUnmarshaller();
        long startTime = System.currentTimeMillis();
        while(true) {
            if (xsr.getEventType() == XMLStreamReader.START_ELEMENT) {
                JAXBElement<Object> jel = unm.unmarshal(xsr, Object.class);
                Node domNode = (Node)jel.getValue();

                NodeList children = domNode.getChildNodes();
                Doc document = new Doc();
                document.ID=count;
                count++;
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    switch (node.getNodeName()){
                        case "title":
                            document.title=node.getTextContent();
                            break;
                        case "url":
                            document.url=node.getTextContent();
                            break;
                        case "abstract":
                            document.text=node.getTextContent();
                            break;
                        default:
                            break;
                    }
                }

                addDoc(document);
                String[] tokensDocument = addIndex(document);
                boolean searchResult = true;
                for (String token: tokensSearch){
                    if (!Arrays.asList(tokensDocument).contains(token)){
                        searchResult=false;
                        break;
                    }
                }

                if (searchResult) {
                    document.showDoc();
                    countFound++;
                }


            } else if (!xsr.hasNext()) {
                break;
            } else {
                xsr.next();
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total found " + countFound);
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        System.out.println("Index addition completed. Search will be faster.");

        while (true){
            enterSearchString();
            search();
        }

    }
}
