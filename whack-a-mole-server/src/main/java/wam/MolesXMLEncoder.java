package wam;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class MolesXMLEncoder {

    public String toXML(long round, GridPosition[] moles) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().newDocument();

        Element molesElem = doc.createElement("moles");
        doc.appendChild(molesElem);

        Element roundElem = doc.createElement("round");
        roundElem.setAttribute("count", String.valueOf(round));
        molesElem.appendChild(roundElem);

        for (GridPosition mole : moles) {
            Element moleElem = doc.createElement("mole");
            moleElem.setAttribute("row", String.valueOf(mole.row));
            moleElem.setAttribute("col", String.valueOf(mole.col));
            molesElem.appendChild(moleElem);
        }

        Transformer transf = TransformerFactory.newInstance().newTransformer();
        transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transf.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StringWriter stringWriter = new StringWriter();
        StreamResult string = new StreamResult(stringWriter);
        transf.transform(source, string);

        return stringWriter.toString();
    }

}
