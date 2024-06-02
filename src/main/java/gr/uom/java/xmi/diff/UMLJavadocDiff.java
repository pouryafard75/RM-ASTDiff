package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLTagElement;

public class UMLJavadocDiff {
	private Set<Pair<UMLTagElement, UMLTagElement>> commonTags;
	private Set<Pair<UMLDocElement, UMLDocElement>> commonDocElements;
	private List<UMLTagElement> deletedTags;
	private List<UMLTagElement> addedTags;
	private List<UMLDocElement> deletedDocElements;
	private List<UMLDocElement> addedDocElements;
	private boolean manyToManyReformat;

	public UMLJavadocDiff(UMLJavadoc javadocBefore, UMLJavadoc javadocAfter) {
		this.commonTags = new LinkedHashSet<Pair<UMLTagElement,UMLTagElement>>();
		this.commonDocElements = new LinkedHashSet<Pair<UMLDocElement,UMLDocElement>>();
		this.deletedTags = new ArrayList<UMLTagElement>();
		this.addedTags = new ArrayList<UMLTagElement>();
		this.deletedDocElements = new ArrayList<UMLDocElement>();
		this.addedDocElements = new ArrayList<UMLDocElement>();
		List<UMLTagElement> tagsBefore = javadocBefore.getTags();
		List<UMLTagElement> tagsAfter = javadocAfter.getTags();
		List<UMLTagElement> deletedTags = new ArrayList<UMLTagElement>(tagsBefore);
		List<UMLTagElement> addedTags = new ArrayList<UMLTagElement>(tagsAfter);
		if(tagsBefore.size() <= tagsAfter.size()) {
			for(UMLTagElement tagBefore : tagsBefore) {
				if(tagsAfter.contains(tagBefore)) {
					int index = tagsAfter.indexOf(tagBefore);
					Pair<UMLTagElement, UMLTagElement> pair = Pair.of(tagBefore, tagsAfter.get(index));
					commonTags.add(pair);
					processIdenticalTags(tagBefore, tagsAfter.get(index));
					deletedTags.remove(tagBefore);
					addedTags.remove(tagBefore);
				}
			}
		}
		else {
			for(UMLTagElement tagAfter : tagsAfter) {
				if(tagsBefore.contains(tagAfter)) {
					int index = tagsBefore.indexOf(tagAfter);
					Pair<UMLTagElement, UMLTagElement> pair = Pair.of(tagsBefore.get(index), tagAfter);
					commonTags.add(pair);
					processIdenticalTags(tagsBefore.get(index), tagAfter);
					deletedTags.remove(tagAfter);
					addedTags.remove(tagAfter);
				}
			}
		}
		List<UMLTagElement> deletedToBeDeleted = new ArrayList<UMLTagElement>();
		List<UMLTagElement> addedToBeDeleted = new ArrayList<UMLTagElement>();
		if(deletedTags.size() <= addedTags.size()) {
			for(UMLTagElement tagBefore : deletedTags) {
				for(UMLTagElement tagAfter : addedTags) {
					boolean match = processModifiedTags(tagBefore, tagAfter);
					if(match) {
						deletedToBeDeleted.add(tagBefore);
						addedToBeDeleted.add(tagAfter);
						break;
					}
				}
			}
		}
		else {
			for(UMLTagElement tagAfter : addedTags) {
				for(UMLTagElement tagBefore : deletedTags) {
					boolean match = processModifiedTags(tagBefore, tagAfter);
					if(match) {
						deletedToBeDeleted.add(tagBefore);
						addedToBeDeleted.add(tagAfter);
						break;
					}
				}
			}
		}
		deletedTags.removeAll(deletedToBeDeleted);
		addedTags.removeAll(addedToBeDeleted);
		this.deletedTags.addAll(deletedTags);
		this.addedTags.addAll(addedTags);
	}

	private void processIdenticalTags(UMLTagElement tagBefore, UMLTagElement tagAfter) {
		List<UMLDocElement> fragmentsBefore = tagBefore.getFragments();
		List<UMLDocElement> fragmentsAfter = tagAfter.getFragments();
		for(int i=0; i<fragmentsBefore.size(); i++) {
			UMLDocElement docElementBefore = fragmentsBefore.get(i);
			UMLDocElement docElementAfter = fragmentsAfter.get(i);
			Pair<UMLDocElement, UMLDocElement> pair = Pair.of(docElementBefore, docElementAfter);
			commonDocElements.add(pair);
		}
	}

	private boolean processModifiedTags(UMLTagElement tagBefore, UMLTagElement tagAfter) {
		int commonDocElementsBefore = commonDocElements.size();
		List<UMLDocElement> fragmentsBefore = tagBefore.getFragments();
		List<UMLDocElement> fragmentsAfter = tagAfter.getFragments();
		List<UMLDocElement> deletedDocElements = new ArrayList<UMLDocElement>(fragmentsBefore);
		List<UMLDocElement> addedDocElements = new ArrayList<UMLDocElement>(fragmentsAfter);
		if(fragmentsBefore.size() <= fragmentsAfter.size()) {
			for(UMLDocElement docElement : fragmentsBefore) {
				if(fragmentsAfter.contains(docElement)) {
					int index = fragmentsAfter.indexOf(docElement);
					Pair<UMLDocElement, UMLDocElement> pair = Pair.of(docElement, fragmentsAfter.get(index));
					commonDocElements.add(pair);
					deletedDocElements.remove(docElement);
					addedDocElements.remove(docElement);
				}
			}
		}
		else {
			for(UMLDocElement docElement : fragmentsAfter) {
				if(fragmentsBefore.contains(docElement)) {
					int index = fragmentsBefore.indexOf(docElement);
					Pair<UMLDocElement, UMLDocElement> pair = Pair.of(fragmentsBefore.get(index), docElement);
					commonDocElements.add(pair);
					deletedDocElements.remove(docElement);
					addedDocElements.remove(docElement);
				}
			}
		}
		List<UMLDocElement> deletedToBeDeleted = new ArrayList<UMLDocElement>();
		List<UMLDocElement> addedToBeDeleted = new ArrayList<UMLDocElement>();
		if(deletedDocElements.size() == addedDocElements.size()) {
			for(int i=0; i<deletedDocElements.size(); i++) {
				UMLDocElement deletedDocElement = deletedDocElements.get(i);
				UMLDocElement addedDocElement = addedDocElements.get(i);
				if(deletedDocElement.getText().replaceAll("\\s", "").equals(addedDocElement.getText().replaceAll("\\s", ""))) {
					Pair<UMLDocElement, UMLDocElement> pair = Pair.of(deletedDocElement, addedDocElement);
					commonDocElements.add(pair);
					deletedToBeDeleted.add(deletedDocElement);
					addedToBeDeleted.add(addedDocElement);
				}
			}
			deletedDocElements.removeAll(deletedToBeDeleted);
			addedDocElements.removeAll(addedToBeDeleted);
		}
		//check if all deleted docElements match all added docElements
		StringBuilder deletedSB = new StringBuilder();
		for(UMLDocElement deletedDocElement : deletedDocElements) {
			String text = deletedDocElement.getText();
			deletedSB.append(text);
		}
		StringBuilder addedSB = new StringBuilder();
		for(UMLDocElement addedDocElement : addedDocElements) {
			String text = addedDocElement.getText();
			addedSB.append(text);
		}
		if(deletedSB.toString().replaceAll("\\s", "").equals(addedSB.toString().replaceAll("\\s", ""))) {
			//make all pair combinations
			for(UMLDocElement deletedDocElement : deletedDocElements) {
				for(UMLDocElement addedDocElement : addedDocElements) {
					Pair<UMLDocElement, UMLDocElement> pair = Pair.of(deletedDocElement, addedDocElement);
					commonDocElements.add(pair);
				}
			}
			if(deletedDocElements.size() >= 1 && addedDocElements.size() >= 1) {
				manyToManyReformat = true;
			}
			return true;
		}
		if(deletedDocElements.size() > addedDocElements.size()) {
			for(UMLDocElement addedDocElement : addedDocElements) {
				String text = addedDocElement.getText();
				for(int i=0; i<deletedDocElements.size()-1; i++) {
					List<UMLDocElement> matches = findConcatenatedMatch(deletedDocElements, text, i);
					if(matches.size() > 0) {
						for(UMLDocElement match : matches) {
							Pair<UMLDocElement, UMLDocElement> pair = Pair.of(match, addedDocElement);
							commonDocElements.add(pair);
							deletedToBeDeleted.add(match);
						}
						addedToBeDeleted.add(addedDocElement);
						break;
					}
				}
			}
		}
		else {
			for(UMLDocElement deletedDocElement : deletedDocElements) {
				String text = deletedDocElement.getText();
				for(int i=0; i<addedDocElements.size()-1; i++) {
					List<UMLDocElement> matches = findConcatenatedMatch(addedDocElements, text, i);
					if(matches.size() > 0) {
						for(UMLDocElement match : matches) {
							Pair<UMLDocElement, UMLDocElement> pair = Pair.of(deletedDocElement, match);
							commonDocElements.add(pair);
							addedToBeDeleted.add(match);
						}
						deletedToBeDeleted.add(deletedDocElement);
						break;
					}
				}
			}
		}
		deletedDocElements.removeAll(deletedToBeDeleted);
		addedDocElements.removeAll(addedToBeDeleted);
		this.deletedDocElements.addAll(deletedDocElements);
		this.addedDocElements.addAll(addedDocElements);
		if(commonDocElements.size() > commonDocElementsBefore) {
			return true;
		}
		return false;
	}

	private List<UMLDocElement> findConcatenatedMatch(List<UMLDocElement> docElements, String text, int startIndex) {
		StringBuilder concatText = new StringBuilder();
		for(int i=startIndex; i<docElements.size(); i++) {
			concatText.append(docElements.get(i).getText());
			if(concatText.toString().replaceAll("\\s", "").equals(text.replaceAll("\\s", ""))) {
				return new ArrayList<>(docElements.subList(startIndex, i+1));
			}
		}
		return Collections.emptyList();
	}

	public Set<Pair<UMLTagElement, UMLTagElement>> getCommonTags() {
		return commonTags;
	}

	public Set<Pair<UMLDocElement, UMLDocElement>> getCommonDocElements() {
		return commonDocElements;
	}

	public List<UMLTagElement> getDeletedTags() {
		return deletedTags;
	}

	public List<UMLTagElement> getAddedTags() {
		return addedTags;
	}

	public List<UMLDocElement> getDeletedDocElements() {
		return deletedDocElements;
	}

	public List<UMLDocElement> getAddedDocElements() {
		return addedDocElements;
	}

	public boolean isManyToManyReformat() {
		return manyToManyReformat;
	}
}