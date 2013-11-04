package cost.cloud;

import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
	private Map<String, String> feeExprs(String xml)
			throws ParserConfigurationException, SAXException, IOException {
		Map<String, String> exprs = new HashMap<String, String>();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xml));
		Document doc = builder.parse(is);

		NodeList fees = doc.getElementsByTagName("fee");

		for (int i = 0; i < fees.getLength(); i++) {
			Element e = (Element) fees.item(i);
			String name = e.getElementsByTagName("feeName").item(0)
					.getTextContent();
			String rule = e.getElementsByTagName("feeExpr").item(0)
					.getTextContent();
			exprs.put(name, rule);
		}

		return exprs;
	}

	// fees 有向图邻接表
	private Map<String, List<String>> adj(Map<String, String> fees) {		
		Map<String, List<String>> neighbors = new HashMap<String, List<String>>();

		for (Entry<String, String> entry : fees.entrySet()) {
			String name = entry.getKey();
			String rule = entry.getValue();

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

		return neighbors;
	}

	// fees 求值拓扑排序
	private List<String> topSort(Map<String, List<String>> neighbors) {
		Jedis jedis = new Jedis("localhost");
		String strAdj = neighbors.toString();
		List<String> sort = jedis.lrange(strAdj, 0, -1);
		if(! sort.isEmpty()){
			return sort;
		}
		
		Map<String, Integer> degree = new HashMap<String, Integer>();

		for (String String : neighbors.keySet()) {
			degree.put(String, 0);
		}

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
		
		
		if(! jedis.exists(strAdj)){
			jedis.rpush(strAdj, calcOrders.toArray(new String[0]));
		}	
		return calcOrders;
	}

	private Map<String, Double> calc(Map<String, String> feeExprs,
			Map<String, List<String>> adj, List<String> calcOrders)
			throws UnknownFunctionException, UnparsableExpressionException {
		Map<String, Double> results = new HashMap<String, Double>();

		for (int i = 0; i < calcOrders.size(); i++) {
			String feeName = calcOrders.get(i);
			String feeExpr = feeExprs.get(feeName);

			for (String refFee : adj.get(feeName)) {
				String target = "[" + refFee + "]";
				String replacement = Double.toString(results.get(refFee));
				feeExpr = feeExpr.replace(target, replacement);
			}

			Calculable calc = new ExpressionBuilder(feeExpr).build();
			double feeResult = calc.calculate();

			results.put(feeName, feeResult);
		}

		return results;
	}

	private String toXml(Map<String, Double> results) {
		StringBuilder xml = new StringBuilder();
		xml.append("<fees>");
		for (Entry<String, Double> entry : results.entrySet()) {
			xml.append("<feeName>");
			xml.append(entry.getKey());
			xml.append("</feeName>");

			xml.append("<feeResult>");
			xml.append(entry.getValue());
			xml.append("</feeResult>");
		}
		xml.append("</fees>");

		return xml.toString();
	}

	public String calculate(String xml) throws Exception {
		Map<String, String> feeExprs = feeExprs(xml);

		Map<String, List<String>> neighbors = adj(feeExprs);

		List<String> calcOrders = topSort(neighbors);
		if (null == calcOrders) {
			throw new Exception("DAG");
		}

		Map<String, Double> results = calc(feeExprs, neighbors, calcOrders);

		return toXml(results);
	}
	
	public static void main(String[] args){
		String xml = "<fees><fee><feeName>人工费</feeName><feeExpr>120</feeExpr></fee><fee><feeName>人工费合价</feeName><feeExpr>[人工费]*10</feeExpr></fee></fees>";
		Fees fees = new Fees();		
		try {
			String res = fees.calculate(xml);
			System.out.print(res);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	public void addjob(String db, String id, int depth) {
		Jedis jedis = new Jedis("localhost");
		jedis.sadd("todo-jobs", db);

		if (null == jedis.zscore(db, id)) {
			jedis.zadd(db, depth, id);
		}
	}
}
