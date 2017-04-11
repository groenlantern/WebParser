/*
 * Author: Jean-Pierre Erasmus 
 * E-Mail : groenlantern@gmail.com
 * JSOUP Needed : https://jsoup.org/download
 * org.json Needed : https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.json%22%20AND%20a%3A%22json%22
 * org.json : https://github.com/stleary/JSON-java
 */
package xwebparser01;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.*;

/**
 *
 * Parse https://github.com/egis/handbook/blob/master/Tech-Stack.md
 * List Technologies by Area
 * Output as JSON Object
 * Parse All elements from top to bottom
 * Look for H2 with no class - Areas 
 * Print out elements of table as sub element of h2
 * thead contains the column names
 * tbody contains the data
 */
public class XWebParser01 {

    /**
     * Parse HTML to JSON
     * @param args
     */
    public static void main(String[] args) {
        
        Document doc;
        try {            
            //Parse HTML document from URL
            doc = Jsoup.connect("https://github.com/egis/handbook/blob/master/Tech-Stack.md").get();
            
            //Get Document Title
            String title = doc.title();            
            
            //Load Document as elements
            Element body = doc.body();
                        
            //Load all HTML elements 
            Elements allElements = body.getAllElements();
            
            //Setup JSON Document nad sections
            JSONObject jsonMainObject = new JSONObject();
            jsonMainObject.put("Title", title);
            
            JSONObject jsonAreaObject = null;
            JSONObject jsonRowObject = null;
            ArrayList<String> columnHeadings = null; 
              
            //Loop All HTML Elements
            for (Element elementObj : allElements) {
                String elmTyp = elementObj.tagName();
                
                /**
                 * Look for area Names 
                 * This is a top to bottom search thru the document, each new area name closes of the previous area
                 * and starts a new area
                 */
                if ( elmTyp!= null && elmTyp.equals("h2") ) { 
                    String elmText = elementObj.text();
                    String elmClass = elementObj.attr("class"); //The areas have no class, there are H2 witth a class which is not an area
                    
                    if ( elmClass != null && 
                         elmClass.trim().isEmpty() ) {                       
                        //Add Area to Main JSON - only once we have our first area
                        if ( jsonAreaObject!= null) { 
                            jsonMainObject.append("Area", jsonAreaObject);
                        }
                        //Create new area
                        jsonAreaObject = new JSONObject();
                        jsonAreaObject.put("Name", elmText);
                    }                                        
                }
                
                //Look for area details/data
                if ( jsonAreaObject!= null) { //We must have an active area to search data
                    //Parse the table header for column names
                    if ( elmTyp!= null && elmTyp.equals("thead") ) {                      
                        columnHeadings = new ArrayList<>();
                        for (Element tableHeadRowObj : elementObj.getElementsByTag("tr")) {
                            for (Element tableHeadCellObj : tableHeadRowObj.getElementsByTag("th") ) {
                                columnHeadings.add(tableHeadCellObj.text());
                            }                        
                        }                    
                    }                
                
                    //Parse the table body for area data
                    if ( elmTyp!= null && elmTyp.equals("tbody") ) {                      
                        for (Element tableRowObj : elementObj.getElementsByTag("tr")) {
                            //Create new json row
                            jsonRowObject = new JSONObject();
                            boolean isEmptyRow = true;
                            int x=0;
                            //Look thru tables cells building json row
                            for (Element tableCellObj : tableRowObj.getElementsByTag("td")) {
                                //Make sure we have a valid column name
                                String colName = null;
                                
                                if (columnHeadings!= null && 
                                    x < columnHeadings.size()) { 
                                    colName = columnHeadings.get(x++);
                                }
                                //Do not add columns with no heading
                                if ( colName!= null && !colName.trim().isEmpty() ) { 
                                    jsonRowObject.put(colName, tableCellObj.text());  
                                    
                                    //Make sure this is not an extra empty row in the table
                                    if ( !tableCellObj.text().trim().isEmpty() ) { 
                                        isEmptyRow = false;
                                    }
                                }
                            }
                            //Add data row to area object if not empty row
                            if ( !isEmptyRow ) { 
                                jsonAreaObject.append("Technologies", jsonRowObject); 
                            }
                        }
                    }
                }
            }
            //Add last Area created to main JSON object
            if ( jsonAreaObject!= null) { 
                jsonMainObject.append("Area", jsonAreaObject);
            }
            
            //Print to JSON object to standard out as a formatted JSON Object. 
            int spacesToIndentEachLevel = 2;            
            System.out.println(jsonMainObject.toString(spacesToIndentEachLevel));
            
        } catch (Exception ex) {
            Logger.getLogger(XWebParser01.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}