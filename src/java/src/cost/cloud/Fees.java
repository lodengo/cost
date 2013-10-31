package cost.cloud;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import redis.clients.jedis.Jedis;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;

public class Fees {
	public String calculate(String xml) {
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			Document doc = builder.parse(is);

			// 费用求值拓扑排序
			Map<String, List<String>> neighbors = new HashMap<String, List<String>>();
			NodeList fees = doc.getElementsByTagName("费用");
			// 有向图邻接表
			for (int i = 0; i < fees.getLength(); i++) {
				Element e = (Element) fees.item(i);
				String name = e.getElementsByTagName("费用名称").item(0)
						.getTextContent();
				String rule = e.getElementsByTagName("计算公式").item(0)
						.getTextContent();

				if (!neighbors.containsKey(name)) {
					neighbors.put(name, new ArrayList<String>());
				}

				Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]");
				Matcher matcher = pattern.matcher(rule);
				while (matcher.find()) {
					String s = matcher.group();
					s = s.substring(1, s.length() - 1);

					if (!neighbors.containsKey(s)) {
						neighbors.put(s, new ArrayList<String>());
					}

					neighbors.get(name).add(s);
				}
			}

			Map<String, Integer> degree = new HashMap<String, Integer>();
			for (String String : neighbors.keySet())
				degree.put(String, 0);
			for (String from : neighbors.keySet()) {
				for (String to : neighbors.get(from)) {
					degree.put(to, degree.get(to) + 1);
				}
			}

			Stack<String> zeroVerts = new Stack<String>();
			for (String String : degree.keySet()) {
				if (degree.get(String) == 0) {
					zeroVerts.push(String);
				}
			}

			List<String> calcOrders = new ArrayList<String>();
			while (!zeroVerts.isEmpty()) {
				String String = zeroVerts.pop();
				calcOrders.add(String);

				for (String neighbor : neighbors.get(String)) {
					degree.put(neighbor, degree.get(neighbor) - 1);

					if (degree.get(neighbor) == 0) {
						zeroVerts.push(neighbor);
					}
				}
			}

			// 费用计算循环依赖
			if (calcOrders.size() != neighbors.size()) {
				return null;
			}

			Collections.reverse(calcOrders);

			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			for (int i = 0; i < calcOrders.size(); i++) {
				Element fee = (Element) xpath.evaluate("//费用[费用名称='"
						+ calcOrders.get(i) + "']", doc, XPathConstants.NODE);
				String expr = fee.getElementsByTagName("计算公式").item(0)
						.getTextContent();
				Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]");
				Matcher matcher = pattern.matcher(expr);
				while (matcher.find()) {
					String from = matcher.group();

					String to = ((Element) xpath.evaluate(
							"//费用[费用名称='"
									+ from.substring(1, from.length() - 1)
									+ "']", doc, XPathConstants.NODE))
							.getElementsByTagName("计算结果").item(0)
							.getTextContent();

					expr = expr.replace(from, to);
				}

				Calculable calc = new ExpressionBuilder(expr).build();
				fee.getElementsByTagName("计算结果").item(0)
						.setTextContent(Double.toString(calc.calculate()));
			}

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			String output = writer.getBuffer().toString();		

			return output;
		} catch (ParserConfigurationException | SAXException | IOException
				| TransformerException | XPathExpressionException
				| UnknownFunctionException | UnparsableExpressionException e) {
			// TODO Auto-generated catch block
			return xml;
		}		
	}
	
	public void addjob(String db, String id, int depth){
		Jedis jedis = new Jedis("localhost");
		jedis.sadd("todo-jobs", db);
		
		if(null == jedis.zscore(db, id)){
			jedis.zadd(db, depth, id);
		}		
	}
}
