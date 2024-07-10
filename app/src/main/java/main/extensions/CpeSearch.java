package main.extensions;

import com.ucsl.json.Json;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CpeSearch {
	Tree dictionnary;
	Json test = Json.object();
	HashMap<Integer,Integer> infos = new HashMap<Integer,Integer>();
	public CpeSearch() {
		dictionnary = new Tree();
	}
	
	/**
	 * Permet d'ajouter une CPE à l'arbre. Il s'agit d'une fonction recurcive, qui s'appelle elle meme pour inserer chaque element de la CPE au bon endroit. 
	 * Le premier element de la CPE est donc ajouté à la racine, puis le second est ajouté à l'endroit qui lui correspond si il n'existe pas encore, et ainsi de suite.
	 * @param cpeList une cpe coupée par rapport aux : (Donc cpe.split(":")
	 * @param dictionary L'arbre auquel ajouter la CPE
	 * @param index L'index de l'element traité dans la CPE 
	 */
	private void addToTree(String[] cpeList, Tree dictionary,int index) {
		if(index < cpeList.length-1){
			if(index == 0)
				dictionary.setElement(cpeList[index]);
			addToTree(cpeList,dictionary.getOrCreateFils(cpeList[index+1]/*, index+1*/),index+1);
		}
		else {
			return;
		}
	}
	
	/**
	 * Fonction de debug permettant d'obtenir une hashmap contenant le nombre d'element par niveau du graph 
	 * Pour qu'elle soit remplie, décommenter le code entre balise \/\* \*\/ de la fonction addToTree
	 * @return une hashmap contenant les infos
	 */
	public HashMap<Integer,Integer> getInfos(){
		return infos;
	}
	
	/**
	 * Affiche pour chaque niveau le nombre maximal de fils qu'un element du niveau possède.
	 */
	public void getMaxes() {
		getMaxes(dictionnary);
	}
	
	/**
	 * Fonctionne avec la fonction getMaxes(), meme principe.
	 * @param dict L'arbre sur lequel faire l'opération
	 */
	@SuppressWarnings("unchecked")
	private void getMaxes(Tree dict) {
		ArrayList<Tree> test = dict.getFilsList();
		ArrayList<Tree> newTest = new ArrayList<Tree>();
		ArrayList<Integer> currentList = new ArrayList<Integer>();
		int level = 1;
		
		while(!test.isEmpty()) {
			Tree current = test.remove(0);
			currentList.add(current.getNombreFils());
			for(Tree t : current.getFilsList()) {
				newTest.add(t);
			}
			if(test.isEmpty()) {
				System.out.println("Niveau "+level+", nombre max de fils : "+getMax(currentList));
				currentList.clear();
				test = (ArrayList<Tree>) newTest.clone();
				newTest.clear();
				level = level+1;
			}
		}
	}
	
	/**
	 * Fonction de debug permettant de récupérer l'intégralité des CPE en les recalculant à partir de l'arbre.
	 * Ca permet de valider que les CPE sont correctes. (Et elles le sont)
	 * @return Un tableau de CPE
	 */
	public ArrayList<String> getAllCpeFromTree() {
		return getAllCpeFromTree(dictionnary);
	}
	
	/**
	 * Fonction récurcive qui fonctionne avec getAllCpeFromTree(), meme principe
	 * @param t L'arbre courant à traiter
	 * @return Une partie de la solution complete.
	 */
	public ArrayList<String> getAllCpeFromTree(Tree t) {
		ArrayList<String> result = new ArrayList<String>();
		if(t.hasFils()) {
			for(Tree current : t.getFilsList()) {
				ArrayList<String> currentList = getAllCpeFromTree(current);
				for(String s : currentList) {
					result.add(t.getElement()+":"+s);
				}
			}
		}
		else {
			result.add(t.element);
		}
		return result;
	}
	
	/**
	 * Calcule et renvoie la valeur maximale d'une liste d'int
	 * @param list
	 * @return
	 */
	private int getMax(ArrayList<Integer> list) {
		int currentMax = 0;
		for(Integer i : list) {
			if(i.intValue() > currentMax)
				currentMax = i.intValue();
		}
		return currentMax;
	}
	

	/**
	 * Renvoie l'arbre sous forme de Json
	 * @return L'arbre complet sous forme de Json
	 */
	public Json exportToJson() {
		return exportToJson(dictionnary);
	}
	
	/**
	 * Fonction récurcive qui fonctionne avec exportToJson(), meme principe
	 * @param dict
	 * @return Une partie de la solution
	 */
	/*private  Json exportToJson(Tree dict) {
		Json result = Json.object();
		Json biResult;
		ArrayList<Json> list = new ArrayList<Json>();

		if(dict.hasFils()) {
			for(Tree t : dict.getFilsList()) {
				biResult = exportToJson(t);
				Json current = Json.object();
				current.at(dict.getElement(),biResult);
				list.add(current.at(dict.getElement()));
			}
			result.at(dict.getElement(),list);
		}
		else {
			Json newFeuille = Json.object();
			newFeuille.at("feuille",dict.getElement());
			return newFeuille;
		}
		
		return result;
	}*/
	private  Json exportToJson(Tree dict) {
		Json result = Json.object();
		Json biResult;
		Json test = Json.object();
		if(dict.hasFils()) {
			for(Tree t : dict.getFilsList()) {
				biResult = exportToJson(t);
				if(biResult == null)
					test.at("feuille",t.getElement());
				else
					test.at(t.getElement(),biResult.at(t.getElement()));
			}
			result.at(dict.getElement(),test);
		}
		else {

			return null;
		}
		
		return result;
	}
	/**
	 * Fonction permettant de récupérer les CVE depuis le document XML
	 * @param nodeList la liste des elements XML
	 */
	private  void getCpeList(NodeList nodeList){  
		for (int count = 0; count < nodeList.getLength(); count++){  
			Node elemNode = nodeList.item(count);  
			if (elemNode.getNodeType() == Node.ELEMENT_NODE && elemNode.getNodeName() == "cpe-list"){  
				// get node name and value  
				NodeList cpeList = elemNode.getChildNodes();
				for(int cpeIndex = 0; cpeIndex < cpeList.getLength(); cpeIndex++) {
					Node currentNode = cpeList.item(cpeIndex);
					if (currentNode.hasAttributes()){  
						NamedNodeMap nodeMap = currentNode.getAttributes();  
						for (int i = 0; i < nodeMap.getLength(); i++){  
							Node node = nodeMap.item(i);  
							if(node.getNodeName() == "name")
								addToTree(node.getNodeValue().split(":"), dictionnary,0);

						}  
					} 
				}
			}  
		}  
	}  
	
	/**
	 * Permet de lire le fichier dictionnaire de CPE du NIST et de construire l'arbre en fonction de celui ci.
	 * @param path Chemin vers le fichier
	 * @return Un arbre complet
	 */
	public void readDictionnaryFromXML(String path) throws ParserConfigurationException, SAXException, IOException {
		File file = new File(path);  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
		DocumentBuilder db = dbf.newDocumentBuilder();  
		Document doc = db.parse(file);  
		doc.getDocumentElement().normalize();  
		if (doc.hasChildNodes())   
		{  
			getCpeList(doc.getChildNodes());  
		} 
	}
	
	private Json readJsonFile(String fileName) throws IOException {
		String jsonRaw = "";
		File fichierRegles = new File(fileName);
	    InputStream lecteur = new BufferedInputStream(new FileInputStream(fichierRegles));
	    InputStreamReader ipsr =new InputStreamReader(lecteur);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne=br.readLine())!=null){
           jsonRaw+=ligne+"\n";
        }
        br.close(); 
        return Json.read(jsonRaw);
	}
	
	public void readDictionnaryFromJson(String path) throws ParserConfigurationException, SAXException, IOException {
		Json j = Json.object();
		j = readJsonFile(path);
		dictionnary.setElement("cpe");
		readDictionnaryFromJsonRec(j.get("cpe"),dictionnary);
	}
	
	private void readDictionnaryFromJsonRec(Json j,Tree t){
		Map<String, Json> map =  j.asJsonMap();
		Set<String> set = map.keySet();

		for(String s : map.keySet()) {
			if(!s.contentEquals("feuille")) {
				t.addFils(new Tree(s));
				Json newJson = j.at(s);
				Tree newTree = t.getFils(s);
				readDictionnaryFromJsonRec(newJson, newTree);				
			}
			else {
				t.addFils(new Tree(j.at("feuille").asString()));
			
			}
		}			
	
	}
	
	public ArrayList<String> getPrefix() {
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<Tree> fils = dictionnary.getFilsList();
		for(Tree t : fils) {
			result.add(t.getElement());
		}
		return result;
	}
	
	public ArrayList<String> getVendor(String prefix) {
		ArrayList<String> result = new ArrayList<String>();
		Tree t = dictionnary.getSubTree("cpe:"+prefix+"");
		ArrayList<Tree> fils = t.getFilsList();
		for(Tree tree : fils)
			result.add(tree.element);
		return result;
	}
	
	public ArrayList<String> getProduct(String prefix, String vendor) {
		ArrayList<String> result = new ArrayList<String>();
		Tree t = dictionnary.getSubTree("cpe:"+prefix+":"+vendor);
		ArrayList<Tree> fils = t.getFilsList();
		for(Tree tree : fils)
			result.add(tree.element);
		return result;
	}
	
	public ArrayList<String> getVersion(String prefix, String vendor, String version) {
		ArrayList<String> result = new ArrayList<String>();
		Tree t = dictionnary.getSubTree("cpe:"+prefix+":"+vendor+":"+version);
		ArrayList<Tree> fils = t.getFilsList();
		for(Tree tree : fils)
			result.add(tree.element);
		return result;
	}
	

	
	/**
	 * Classe interne utilisée pour manipuler l'arbre des CVE.
	 * @author Antonin
	 */
	private class Tree{
		private String element;
		private ArrayList<Tree> fils = new ArrayList<Tree>();
		
		public Tree getSubTree(String path) {
			String[] splitedPath = path.split(":");
			if (splitedPath[0].contentEquals("cpe")) {
				Tree currentTree = this;
				for(int i = 1; i < splitedPath.length;i++) {
					currentTree = currentTree.getFils(splitedPath[i]);
					if(currentTree == null) {
						System.err.println("Wrong path given in CPE tree (requested path does not exist)");
						return new Tree();
					}
				}
				return currentTree;
			}
			else {
				return new Tree();
			}
			
		}
		
		public Tree() { 
			infos.put(0, 1);
			this.element = null;
		}
		
		public Tree(String element) {
			this.element = element;
		}
		
		public void setElement(String element) {
			this.element = element;
		}
		
		public ArrayList<Tree> getFilsList() {
			return fils;
		}
		
		public String getElement() {
			return this.element;
		}
		
		@SuppressWarnings("unused")
		public void addFils(String element) {
			Tree newNode = new Tree(element);
			this.fils.add(newNode);
		}
		
		public void addFils(Tree element) {
			this.fils.add(element);
		}	
		
		@SuppressWarnings("unused")
		public Tree getFils(String element) {
			for(Tree cur : this.fils) {
				if(cur.getElement().contentEquals(element))
					return cur;
			}
			return null;
		}

		public Tree getOrCreateFils(String element) {
			for(Tree cur : this.fils) {
				if(cur.getElement().contentEquals(element)) {
					return cur;
				}

			}
			Tree newFils = new Tree(element);
			this.addFils(newFils);
			return newFils;
		}
		
		@SuppressWarnings("unused")
		public Tree getOrCreateFils(String element, int index) {
			for(Tree cur : this.fils) {
				if(cur.getElement().contentEquals(element)) {
					return cur;
				}
			}
	        if(!infos.containsKey(index)) {
	            infos.put(index,1);
	        }
	        else {
	            infos.put(index,infos.get(index)+1);
	        }
			Tree newFils = new Tree(element);
			this.addFils(newFils);
			return newFils;
		}	
		
		public int getNombreFils() {
			return fils.size();
		}
		
		public boolean hasFils() {
			if(this.fils.size() > 0)
				return true;
			return false;
		}

		@SuppressWarnings("unused")
		public void printFils() {
			for(Tree t : this.fils)
				System.out.println(t.getElement());
		}
	}
	
	
	
}
