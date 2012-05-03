/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.utils.StringUtilities;

/**
 * <p>This class encapsulates information pertaining to anatomy of projection x-ray images.</p>
 * 
 * <p>Utility methods provide for the detection of anatomy from various header attributes regardless
 * of whether these are formal codes, code strings or free text comments.</p>
 * 
 * @author	dclunie
 */
public class ProjectionXRayAnatomy {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/ProjectionXRayAnatomy.java,v 1.22 2011/05/04 11:46:35 dclunie Exp $";
	
	protected static String[] newStringArray(String... values) { return values; }		// use 1.5 varargs feature; seems like a lot of trouble to work around lack of string array curly braces outside declarations

	protected static String[] badLateralityOrViewOrAnatomyPhraseTriggers = {
		"History",
		"Hx of"
	};
	
	protected static String[] badLateralityWords = null;
	
	protected static DisplayableLateralityConcept[] lateralityConceptEntries = {
		new DisplayableLateralityConcept(null,	"SRT",	"SRT",	null,	"G-A100",	"Right",			"R",
			newStringArray(
				"RT",
				"Rechts"/*NL*/,
				"DROITE"/*FR*/
			),
			newStringArray("Right"),	newStringArray("Right")),
		new DisplayableLateralityConcept(null,	"SRT",	"SRT",	null,	"G-A101",	"Left",				"L",
			newStringArray(
				"LT",
				"Links"/*NL*/,
				"GAUCHE"/*FR*/
			),
			newStringArray("Right"),	newStringArray("Left")),
		new DisplayableLateralityConcept(null,	"SRT",	"SRT",	null,	"G-A102",	"Right and left",	"B",
			newStringArray(
				"Both"
			),
			newStringArray("Both"),	newStringArray("Both")),
		new DisplayableLateralityConcept(null,	"SRT",	"SRT",	null,	"G-A103",	"Unilateral",		"U",
			newStringArray(
				"Unpaired"
			),
			newStringArray("Unpaired"),	newStringArray("Unpaired")),
	};
	
	protected static DictionaryOfConcepts lateralityConcepts = new DictionaryOfConcepts(lateralityConceptEntries,badLateralityWords,"Laterality");

	protected static String[] badViewWords = null;
	
	protected static DisplayableViewConcept[] viewPositionConceptEntries = {
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-10202",	"frontal",			"FRONTAL"/*non-standard*/,
			newStringArray(
				"Face"/*FR*/
			),
			newStringArray("Frontal"),	newStringArray("Frontal")),
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-10206",	"antero-posterior",	"AP",
			newStringArray(
				"AP",
				"A.P",
				"antero_posterior",
				"anteroposterior"
			),
			newStringArray("AP"),		newStringArray("AP")),
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-10214",	"postero-anterior",	"PA",
			newStringArray(
				"PA",
				"P.A",
				"postero_anterior",
				"posteroanterior"
			),
			newStringArray("PA"),		newStringArray("PA")),
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-102CD",	"lateral",			"LATERAL"/*non-standard*/,	
			newStringArray(
				"Lat",
				"Profil"/*FR*/,
				"Sida"/*SE*/
			),
			newStringArray("Lateral"),	newStringArray("Lateral")),
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-10236",	"left lateral",		"LL",
			newStringArray(
				"LL",
				"LLat",
				"L Lat"
			),
			newStringArray("Left Lateral"),	newStringArray("Left Lateral")),
		new DisplayableViewConcept(null,	"SRT",	"SNM3",	null,	"R-10232",	"right lateral",	"RL",
			newStringArray(
				"RL",
				"RLat",
				"R Lat"
			),
			newStringArray("Right Lateral"),	newStringArray("Right Lateral"))
	};

	protected static DictionaryOfConcepts viewPositionConcepts = new DictionaryOfConcepts(viewPositionConceptEntries,badViewWords,"View");
	
	protected static String[] badAnatomyWords = {
		"research"		// contains "ear"
	};

	protected static DisplayableAnatomicConcept[] anatomicConceptEntries = {
		new DisplayableAnatomicConcept("C0000726",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D4000",	"Abdomen",			"ABDOMEN",
			newStringArray(
				"Abdominal",
				"BØICHO"/*CZ*/,
				"Buik"/*NL*/,
				"Vatsa"/*FI*/,
				"Ventre"/*FR*/,
				"Addome"/*IT*/,
				"Abdome"/*PT*/,
				"はら"/*JP*/,
				"心窩部"/*JP*/,
				"胴"/*JP*/,
				"腹"/*JP*/,
				"腹部"/*JP*/,
				"ЖИВОТ"/*RU*/,
				"Buk"/*NL*/			
			),
			newStringArray("Abdomen"),			newStringArray("Abdomen")),
		new DisplayableAnatomicConcept("23451007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-B3000",	"Adrenal gland",	"ADRENAL",
				newStringArray("Adrenal"),
				newStringArray("Adrenal gland"),	newStringArray("Adrenal gland")),
		new DisplayableAnatomicConcept("70258002"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15750",	"Ankle joint",		"ANKLE",
			newStringArray(
				"Ankle",
				"Tobillo"/*ES*/,
				"Knöchel"/*DE*/,
				"Enkel"/*NL*/,
				"Cheville"/*FR*/,
				"Tornozelo"/*PT*/,
				"αστράγαλος"/*GR*/,
				"足首"/*JP*/,
				"발목"/*KR*/,
				"лодыжка"/*RU*/
			),
			newStringArray("Ankle joint"),		newStringArray("Ankle joint")),
		new DisplayableAnatomicConcept("15825003"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-42000",	"Aorta",			"AORTA",		null,	newStringArray("Aorta"),			newStringArray("Aorta")),
		new DisplayableAnatomicConcept("40983000"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8200",	"Arm",				"ARM",			null,	newStringArray("Arm"),				newStringArray("Arm")),
		new DisplayableAnatomicConcept("34797008"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8100",	"Axilla",			"AXILLA",		null,	newStringArray("Axilla"),			newStringArray("Axilla")),
		new DisplayableAnatomicConcept("77568009"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D2100",	"Back",				"BACK",			null,	newStringArray("Back"),				newStringArray("Back")),
		new DisplayableAnatomicConcept("89837001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-74000",	"Bladder",			"BLADDER",		null,	newStringArray("Bladder"),			newStringArray("Bladder")),
		new DisplayableAnatomicConcept("12738006"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-A0100",	"Brain",			"BRAIN",		null,	newStringArray("Brain"),			newStringArray("Brain")),
		new DisplayableAnatomicConcept("76752008"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-04000",	"Breast",			"BREAST",		null,	newStringArray("Breast"),			newStringArray("Breast")),
		new DisplayableAnatomicConcept("955009"/*This is SNOMED ID, not CUI*/,		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-26000",	"Bronchus",			"BRONCHUS",		null,	newStringArray("Bronchus"),			newStringArray("Bronchus")),
		new DisplayableAnatomicConcept("46862004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D2600",	"Buttock",			"BUTTOCK",		null,	newStringArray("Buttock"),			newStringArray("Buttock")),
		new DisplayableAnatomicConcept("80144004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12770",	"Calcaneus",		"CALCANEUS",	null,	newStringArray("Calcaneus"),		newStringArray("Calcaneus")),
		new DisplayableAnatomicConcept("53840002"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9440",	"Calf of leg",		"CALF",
			newStringArray("Calf"),
			newStringArray("Calf of leg"),		newStringArray("Calf of leg")),
		new DisplayableAnatomicConcept("69105007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-45010",	"Carotid Artery",	"CAROTID",
			newStringArray("Carotid"),
			newStringArray("Carotid Artery"),	newStringArray("Carotid Artery")),
		new DisplayableAnatomicConcept("180924008"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-A600A",	"Cerebellum",		"CEREBELLUM",	null,	newStringArray("Cerebellum"),		newStringArray("Cerebellum")),
		new DisplayableAnatomicConcept("C0728985",									false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11501",	"Cervical spine",	"CSPINE",
			newStringArray(
				"CS",
				"CWK"/*NL*/,
				"CWZ"/*NL*/,
				"HWS"/*DE*/,
				"H Rygg"/*SE*/,
				"Cspine",
				"C spine",
				"Spine Cervical",
				"Cervical",
				"Cervic"/*abbrev*/,
				"Kaelalülid"/*EE*/,
				"KRÈNÍ OBRATLE"/*CZ*/,
				"Halswervels"/*NL*/,
				"Vertebrae cervicalis"/*NL*/,
				"Wervel hals"/*NL*/,
				"Kaulanikamat"/*FI*/,
				"Rachis cervical"/*FR*/,
				"Vertèbre cervicale"/*FR*/,
				"Vertèbres cervicales"/*FR*/,
				"COLONNE CERVICALE"/*FR*/,
				"CERVICALE"/*FR*/,
				"Halswirbel"/*DE*/,
				"Vertebrae cervicales"/*DE*/,
				"Vertebre cervicali"/*IT*/,
				"頚椎"/*JP*/,
				"頸椎"/*JP*/,
				"Vértebras Cervicais"/*PT*/,
				"ШЕЙНЫЕ ПОЗВОНКИ"/*RU*/,
				"columna cervical"/*ES*/,
				"columna cerv"/*ES abbrev*/,
				"columna espinal cervical"/*ES*/,
				"columna vertebral cervical"/*ES*/,
				"vértebras cervicales"/*ES*/,
				"Cervikalkotor"/*SE*/,
				"Halskotor"/*SE*/,
				"Halsrygg"/*SE*/,
				"Cervicale wervelzuil"/*BE*/,
				"C chrbtica"/*SK*/
			),
			newStringArray("Cervical spine"),	newStringArray("Cervical spine")),
		new DisplayableAnatomicConcept(null,									false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F7",	"Cervico-thoracic spine",	"CTSPINE",
			newStringArray(
				"CTSPINE",
				"Cervico-thoracic",
				"Cervicothoracic"
			),
			newStringArray("Cervico-thoracic spine"),	newStringArray("Cervico-thoracic spine")),
		new DisplayableAnatomicConcept("71252005"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-83200",	"Cervix",			"CERVIX",		null,	newStringArray("Cervix"),			newStringArray("Cervix")),
		new DisplayableAnatomicConcept("60819002"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1206",	"Cheek",			"CHEEK",		null,	newStringArray("Cheek"),			newStringArray("Cheek")),
		new DisplayableAnatomicConcept("C0817096"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D3000",	"Chest",			"CHEST",
			newStringArray(
				"Thorax",
				"Rindkere"/*EE*/,
				"HRUDNÍK"/*CZ*/,
				"Borst"/*NL*/,
				"Rintakehä"/*FI*/,
				"Poitrine"/*FR*/,
				"Potter"/*FR ?? - seen in examples*/,
				"Torse"/*FR*/,
				"Brustkorb"/*DE*/,
				"Torace"/*IT*/,
				"Peito"/*PT*/,
				"ГРУДНАЯ КЛЕТКА"/*RU*/,
				"ГРУДЬ"/*RU*/,
				"pecho"/*ES*/,
				"torácico"/*ES*/,
				"Bröstkorg"/*SE*/,
				"Torax"/*SE,PT,ES*/,
				"hrudnнk"/*SK*/,
				"hrudn"/*SK abbrev*/
			),
			newStringArray("Chest"),			newStringArray("Chest")),
		new DisplayableAnatomicConcept("362047009"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-45526",	"Circle of Willis",	"CIRCLEOFWILLIS",	null,	newStringArray("Circle of Willis"),	newStringArray("Circle of Willis")),
		new DisplayableAnatomicConcept("51299004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12310",	"Clavicle",			"CLAVICLE",		null,	newStringArray("Clavicle"),			newStringArray("Clavicle")),
		new DisplayableAnatomicConcept("64688005"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11BF0",	"Coccyx",			"COCCYX",		null,	newStringArray("Coccyx"),			newStringArray("Coccyx")),
		new DisplayableAnatomicConcept("71854001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-59300",	"Colon",			"COLON",		null,	newStringArray("Colon"),			newStringArray("Colon")),
		new DisplayableAnatomicConcept("28726007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA200",	"Cornea",			"CORNEA",		null,	newStringArray("Cornea"),			newStringArray("Cornea")),
		new DisplayableAnatomicConcept("41801008"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-43000",	"Coronary artery",	"CORONARYARTERY",
			newStringArray("Coronary"),
			newStringArray("Coronary artery"),	newStringArray("Coronary artery")),
		new DisplayableAnatomicConcept("38848004"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58200",	"Duodenum",			"DUODENUM",		null,	newStringArray("Duodenum"),			newStringArray("Duodenum")),
		new DisplayableAnatomicConcept("1910005"/*This is SNOMED ID, not CUI*/,		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AB000",	"Ear",				"EAR",			null,	newStringArray("Ear"),				newStringArray("Ear")),
		new DisplayableAnatomicConcept("76248009"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8300",	"Elbow",			"ELBOW",
			newStringArray(
				"Ellbogen"/*DE*/,
				"Coude"/*FR*/,
				"Küünar"/*EE*/,
				"Armbåge"/*SE*/,
				"Codo"/*ES*/,
				"Cotovelo"/*PT*/
			),
			newStringArray("Elbow"),			newStringArray("Elbow")),
		new DisplayableAnatomicConcept("38266002"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D0010",	"Entire body",		"WHOLEBODY",
			newStringArray(
				"Entire body",
				"Whole body"
			),
			newStringArray("Entire body"),		newStringArray("Entire body")),
		new DisplayableAnatomicConcept("32849002"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-56000",	"Esophagus",		"ESOPHAGUS",	null,	newStringArray("Esophagus"),		newStringArray("Esophagus")),
		new DisplayableAnatomicConcept("66019005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D0300",	"Extremity",		"EXTREMITY",
			newStringArray(
				"Extremety"/*Agfa CR speljling mistake*/,
				"Extremidad"/*ES*/
				),
			newStringArray("Extremity"),		newStringArray("Extremity")),
		new DisplayableAnatomicConcept("81745001"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA000",	"Eye",				"EYE",			null,	newStringArray("Eye"),				newStringArray("Eye")),
		new DisplayableAnatomicConcept("80243003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA810",	"Eyelid",			"EYELID",		null,	newStringArray("Eyelid"),			newStringArray("Eyelid")),
		// not face ... gets confused with frontal view (FR,NL) ... new DisplayableAnatomicConcept("89545001"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1200",	"Face",				"FACE",			null,	newStringArray("Face"),				newStringArray("Face")),
		new DisplayableAnatomicConcept("71341001"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12710",	"Femur",			"FEMUR",		null,	newStringArray("Femur"),			newStringArray("Femur")),
		new DisplayableAnatomicConcept("7569003"/*This is SNOMED ID, not CUI*/,		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8800",	"Finger",			"FINGER",		null,	newStringArray("Finger"),			newStringArray("Finger")),
		new DisplayableAnatomicConcept("56459004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9700",	"Foot",				"FOOT",
			newStringArray(
				"Pied"/*FR*/,
				"Pie"/*ES*/,
				"Voet"/*NL*/,
				"Fuß"/*DE*/,
				"πόδι"/*GR*/,
				"Piede"/*IT*/,
				"pé"/*PT*/,
				"нога"/*RU*/
			),
			newStringArray("Foot"),				newStringArray("Foot")),
		new DisplayableAnatomicConcept("55797009"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12402",	"Forearm bone",		"FOREARM",
			newStringArray(
				"Forearm",
				"U ARM"/*DE*/,
				"Unterarm"/*DE*/,
				"Avambraccio"/*IT*/,
				"PØEDLOKTÍ"/*CZ*/,
				"Onderarm"/*NL*/,
				"Kyynärvarsi"/*FI*/,
				"Avant-bras"/*FR*/,
				"まえうで"/*JP*/,
				"前腕"/*JP*/,
				"Antebraço"/*PT*/,
				"ПРЕДПЛЕЧЬЕ"/*RU*/,
				"antebrazo"/*ES*/,
				"Underarm"/*SE*/,
				"predlaktie"/*SK*/
			),
			newStringArray("Forearm"),		newStringArray("Forearm")),
		new DisplayableAnatomicConcept("28231008"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-63000",	"Gallbladder",		"GALLBLADDER",	null,	newStringArray("Gallbladder"),		newStringArray("Gallbladder")),
		new DisplayableAnatomicConcept("85562004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8700",	"Hand",				"HAND",			null,	newStringArray("Hand"),				newStringArray("Hand")),
		new DisplayableAnatomicConcept("69536005"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1100",	"Head",				"HEAD",
			newStringArray(
				"Schaedel"/*DE*/,
				"Schædel"/*DE*/,
				"Tete"/*FR*/
			),
			newStringArray("Head"),				newStringArray("Head")),
		new DisplayableAnatomicConcept("774007"/*This is SNOMED ID, not CUI*/,		false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1000",	"Head and Neck",	"HEADNECK",	
			newStringArray("Head Neck"),
			newStringArray("Head and Neck"),	newStringArray("Head and Neck")),
		new DisplayableAnatomicConcept("80891009"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-32000",	"Heart",			"HEART",		null,	newStringArray("Heart"),			newStringArray("Heart")),
		new DisplayableAnatomicConcept("24136001"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15710",	"Hip joint",		"HIP",
			newStringArray(
				"Hip",
				"Heup"/*NL*/,
				"Hanche"/*FR*/,
				"Hüfte"/*DE*/,
				"Puus"/*EE*/,
				"HÖFT"/*SE*/,
				"Cadera"/*ES*/,
				"ισχίο"/*GR*/,
				"anca"/*IT*/,
				"ヒップ"/*JP*/,
				"엉덩이"/*KR*/,
				"вальма"/*RU*/
			),
			newStringArray("Hip"),		newStringArray("Hip")),
		new DisplayableAnatomicConcept("85050009"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12410",	"Humerus",			"HUMERUS",
			newStringArray(
				"UP_EXM"/*Fuji CR BPE*/,
				"O ARM"/*DE,SE*/,
				"Oberarm"/*DE*/,
				"Õlavars"/*EE*/,
				"Bovenarm"/*NL*/,
				"húmero"/*ES*/
			),
			newStringArray("Humerus"),			newStringArray("Humerus")),
		new DisplayableAnatomicConcept("34516001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58600",	"Ileum",			"ILEUM",		null,	newStringArray("Ileum"),			newStringArray("Ileum")),
		new DisplayableAnatomicConcept("22356005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12340",	"Ilium",			"ILIUM",		null,	newStringArray("Ilium"),			newStringArray("Ilium")),
		new DisplayableAnatomicConcept("361078006"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AB959",	"Internal Auditory Canal",	"IAC",
			newStringArray("IAC"),
			newStringArray("Internal Auditory Canal"),	newStringArray("Internal Auditory Canal")),
		new DisplayableAnatomicConcept("661005"/*This is SNOMED ID, not CUI*/,		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1213",	"Jaw region",		"JAW",			null,	newStringArray("Jaw region"),		newStringArray("Jaw region")),
		new DisplayableAnatomicConcept("21306003"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58400",	"Jejunum",			"JEJUNUM",		null,	newStringArray("Jejunum"),			newStringArray("Jejunum")),
		new DisplayableAnatomicConcept("64033007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-71000",	"Kidney",			"KIDNEY",		null,	newStringArray("Kidney"),			newStringArray("Kidney")),
		new DisplayableAnatomicConcept("72696002"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9200",	"Knee",				"KNEE",
			newStringArray(
				"Knie"/*DE,NL*/,
				"Genou"/*FR*/,
				"Põlv"/*EE*/,
				"Pölv"/*EE ?wrong accent*/,
				"Knä"/*SE*/,
				"Rodilla"/*ES*/
			),
			newStringArray("Knee"),				newStringArray("Knee")),
		new DisplayableAnatomicConcept("4596009"/*This is SNOMED ID, not CUI*/,		false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-24100",	"Larynx",			"LARYNX",		null,	newStringArray("Larynx"),			newStringArray("Larynx")),
		new DisplayableAnatomicConcept("30021000"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9400",	"Leg",				"LEG",
			newStringArray(
				"LOW_EXM"/*Fuji CR BPE*/,
				"LOWEXM"/*Siemens CR BPE*/,
				"TIB FIB ANKLE",
				"Jambe"/*FR*/
			),
			newStringArray("Leg"),				newStringArray("Leg")),
		new DisplayableAnatomicConcept("10200004"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-62000",	"Liver",			"LIVER",		null,	newStringArray("Liver"),			newStringArray("Liver")),
		new DisplayableAnatomicConcept("C0024091",									false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11503",	"Lumbar spine",		"LSPINE",
			newStringArray(
				"LS",
				"LWK"/*NL*/,
				"LWZ"/*NL*/,
				"LWS"/*DE*/,
				"L Rygg"/*SE*/,
				"Lspine",
				"L spine",
				"Spine Lumbar",
				"Lumbar",
				"Rachis lombaire"/*FR*/,
				"COLONNE LOMBAIRE"/*FR*/,
				"Rach.Lomb"/*FR abbrev*/,
				"lombaire"/*FR*/,
				"Nimmelülid"/*EE*/,
				"Columna lumbar"/*ES*/,
				"LÄNDRYGG"/*SE*/,
				"L chrbtica"/*SK*/,
				"COL LOMBARE"
			),
			newStringArray("Lumbar spine"),		newStringArray("Lumbar spine")),
		new DisplayableAnatomicConcept("C0223603",										false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F9",	"Lumbo-sacral spine",	"LSSPINE",
			newStringArray(
				"LSSPINE",
				"Lumbosacral spine",
				"Lumbo-sacrale wervelzuil"/*BE*/,
				"columna vertebral lumbosacra"/*ES*/,
				"vértebras lumbosacras"/*ES*/,
				"Colonna Lombosacrale"
			),
			newStringArray("Lumbo-sacral spine"),	newStringArray("Lumbo-sacral spine")),
		new DisplayableAnatomicConcept("39607008"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-28000",	"Lung",				"LUNG",			null,	newStringArray("Lung"),				newStringArray("Lung")),
		new DisplayableAnatomicConcept("91609006"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11180",	"Mandible",			"JAW",			null,	newStringArray("Mandible"),			newStringArray("Mandible")),
		new DisplayableAnatomicConcept("70925003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11170",	"Maxilla",			"MAXILLA",		null,	newStringArray("Maxilla"),			newStringArray("Maxilla")),
		new DisplayableAnatomicConcept("72410000"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D3300",	"Mediastinum",		"MEDIASTINUM",	null,	newStringArray("Mediastinum"),		newStringArray("Mediastinum")),
		new DisplayableAnatomicConcept("21082005"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-51000",	"Mouth",			"MOUTH",		null,	newStringArray("Mouth"),			newStringArray("Mouth")),
		new DisplayableAnatomicConcept("45048000"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1600",	"Neck",				"NECK",
			newStringArray(
				"Kael"/*EE*/
			),
			newStringArray("Neck"),				newStringArray("Neck")),
		new DisplayableAnatomicConcept("45206002"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-21000",	"Nose",				"NOSE",			null,	newStringArray("Nose"),				newStringArray("Nose")),
		new DisplayableAnatomicConcept("371398005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D0801",	"Orbital region",	"ORBIT",
			newStringArray(
				"Orbit"
			),
			newStringArray("Orbital region"),	newStringArray("Orbital region")),
		new DisplayableAnatomicConcept("15497006"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-87000",	"Ovary",			"OVARY",		null,	newStringArray("Ovary"),			newStringArray("Ovary")),
		new DisplayableAnatomicConcept("181277001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D4034",	"Pancreas",			"PANCREAS",		null,	newStringArray("Pancreas"),			newStringArray("Pancreas")),
		new DisplayableAnatomicConcept("45289007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-61100",	"Parotid gland",	"PAROTID",
			newStringArray("Parotid"),
			newStringArray("Parotid gland"),	newStringArray("Parotid gland")),
		new DisplayableAnatomicConcept("64234005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12730",	"Patella",			"PATELLA",		null,	newStringArray("Patella"),			newStringArray("Patella")),
		new DisplayableAnatomicConcept("12921003"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D6000",	"Pelvis",			"PELVIS",
			newStringArray(
				"Bekken"/*NL*/,
				"Becken"/*DE*/,
				"Bassin"/*FR*/,
				"Vaagen"/*EE*/,
				"BÄCKEN"/*SE*/,
				"λεκάνη"/*GR*/,
				"Bacino"/*IT*/,
				"骨盤"/*JP*/,
				"골반"/*KR*/
			),
			newStringArray("Pelvis"),			newStringArray("Pelvis")),
		new DisplayableAnatomicConcept("18911002"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-91000",	"Penis",			"PENIS",		null,	newStringArray("Penis"),			newStringArray("Penis")),
		new DisplayableAnatomicConcept("181211006"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-55002",	"Pharynx",			"PHARYNX",		null,	newStringArray("Pharynx"),			newStringArray("Pharynx")),
		new DisplayableAnatomicConcept("181422007"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-9200B",	"Prostate",			"PROSTATE",		null,	newStringArray("Prostate"),			newStringArray("Prostate")),
		new DisplayableAnatomicConcept("34402009"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-59600",	"Rectum",			"RECTUM",		null,	newStringArray("Rectum"),			newStringArray("Rectum")),
		new DisplayableAnatomicConcept("113197003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11300",	"Rib",				"RIB",
			newStringArray(
				"Gril costal"/*FR*/,
				"Gril cost"/*FR abbrev*/
			),
			newStringArray("Rib"),				newStringArray("Rib")),
		new DisplayableAnatomicConcept("54735007"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11AD0",	"Sacrum",			"SSPINE",
			newStringArray("SSPINE"),
			newStringArray("Sacrum"),			newStringArray("Sacrum")),
		new DisplayableAnatomicConcept("41695006"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1160",	"Scalp",			"SCALP",		null,	newStringArray("Scalp"),			newStringArray("Scalp")),
		new DisplayableAnatomicConcept("79601000"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12280",	"Scapula",			"SCAPULA",		null,	newStringArray("Scapula"),			newStringArray("Scapula")),
		new DisplayableAnatomicConcept("18619003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA110",	"Sclera",			"SCLERA",		null,	newStringArray("Sclera"),			newStringArray("Sclera")),
		new DisplayableAnatomicConcept("20233005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-98000",	"Scrotum",			"SCROTUM",		null,	newStringArray("Scrotum"),			newStringArray("Scrotum")),
		new DisplayableAnatomicConcept("16982005"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D2220",	"Shoulder",			"SHOULDER",
			newStringArray(
				"Schouder"/*NL*/,
				"Schulter"/*DE*/,
				"Epaule"/*FR*/,
				"épaule"/*FR*/,
				"õlg"/*EE*/,
				"Ölg"/*EE ?wrong accent*/,
				"Hombro"/*ES*/,
				"Ombro"/*PT*/,
				"Rameno"/*SK*/,
				"Rippe"/*DE*/
			),
			newStringArray("Shoulder"),			newStringArray("Shoulder")),
		new DisplayableAnatomicConcept("C0037303",									false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11100",	"Skull",			"SKULL",
			newStringArray(
				"Kolju"/*EE*/,
				"LEBKA"/*CZ*/,
				"Schedel"/*NL*/,
				"Kallo"/*FI*/,
				"Crâne"/*FR*/,
				"Cranium"/*DE*/,
				"Schädel"/*DE*/,
				"Cranio"/*IT*/,
				"Calota Craniana"/*PT*/,
				"Crânio"/*PT*/,
				"ЧЕРЕП"/*RU*/,
				"Calota Craneal"/*ES*/,
				"Cráneo"/*ES*/,
				"Kalvarium"/*SE*/,
				"Kranium"/*SE*/,
				"Skalle"/*SE*/,
				"Lebka"/*SK*/
			),
			newStringArray("Skull"),			newStringArray("Skull")),
		new DisplayableAnatomicConcept("280717001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D0146",	"Spine",			"SPINE",
			newStringArray(
				"Rachis"/*FR*/,
				"Rygg"/*SE*/,
				"chrbtica"/*SK*/
			),
			newStringArray("Spine"),			newStringArray("Spine")),
		new DisplayableAnatomicConcept("78961009"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-C3000",	"Spleen",			"SPLEEN",		null,	newStringArray("Spleen"),			newStringArray("Spleen")),
		new DisplayableAnatomicConcept("56873002"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11210",	"Sternum",			"STERNUM",		null,	newStringArray("Sternum"),			newStringArray("Sternum")),
		new DisplayableAnatomicConcept("69695003"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-57000",	"Stomach",			"STOMACH",		null,	newStringArray("Stomach"),			newStringArray("Stomach")),
		new DisplayableAnatomicConcept("54019009"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-61300",	"Submandibular gland",	"SUBMANDIBULAR",
			newStringArray("Submandibular"),
			newStringArray("Submandibular gland"),	newStringArray("Submandibular gland")),
		new DisplayableAnatomicConcept("53620006"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15290",	"Temporomandibular joint",	"TMJ",
			newStringArray(
				"Temporomandibular",
				"TMJ"
			),
			newStringArray("Temporomandibular joint"),	newStringArray("Temporomandibular joint")),
		new DisplayableAnatomicConcept("40689003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-94000",	"Testis",			"TESTIS",		null,	newStringArray("Testis"),			newStringArray("Testis")),
		new DisplayableAnatomicConcept("68367000"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9100",	"Thigh",			"THIGH",
			newStringArray(
				"Oberschenkel"/*DE*/,
				"Bovenbeen"/*NL*/,
				"Reis"/*EE*/
			),
			newStringArray("Thigh"),			newStringArray("Thigh")),
		new DisplayableAnatomicConcept("C0581269",									false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11502",	"Thoracic spine",	"TSPINE",
			newStringArray(
				"TSPINE",
				"TS",
				"THWK"/*NL*/,
				"DWZ"/*NL*/,
				"BWS"/*DE*/,
				"B Rygg"/*SE*/,
				"T spine",
				"Spine Thoracic",
				"Thoracic",
				"Dorsal",
				"Dorsal spine",
				"Spine Dorsal",
				"Rachis dorsal"/*FR*/,
				"COLONNE THORACIQUE"/*FR*/,
				"THORACIQUE"/*FR*/,
				"Rinnaosa"/*EE??*/,
				"Rinnalülid"/*EE*/,
				"Columna dorsal"/*ES*/,
				"Columna vertebral dorsal"/*ES*/,
				"Thoracale wervelzuil"/*BE*/,
				"BRÖSTRYGG"/*SE*/,
				"Th chrbtica"/*SK*/
			),
			newStringArray("Thoracic spine"),	newStringArray("Thoracic spine")),
		new DisplayableAnatomicConcept(null,									false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F8",	"Thoraco-lumbar spine",	"TLSPINE",
			newStringArray(
				"TLSPINE",
				"Thoraco-lumbar",
				"Thoracolumbar",
				"Col.Dors.Lomb"/*FR abbrev*/,
				"THORACOLUMBALE"
			),
			newStringArray("Thoraco-lumbar spine"),	newStringArray("Thoraco-lumbar spine")),
		new DisplayableAnatomicConcept("76505004"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8810",	"Thumb",			"THUMB",		null,	newStringArray("Thumb"),			newStringArray("Thumb")),
		new DisplayableAnatomicConcept("118507000"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-C8001",	"Thymus",			"THYMUS",		null,	newStringArray("Thymus"),			newStringArray("Thymus")),
		new DisplayableAnatomicConcept("69748006"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-B6000",	"Thyroid",			"THYROID",		null,	newStringArray("Thyroid"),			newStringArray("Thyroid")),
		new DisplayableAnatomicConcept("29707007"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9800",	"Toe",				"TOE",			null,	newStringArray("Toe"),				newStringArray("Toe")),
		new DisplayableAnatomicConcept("21974007"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-53000",	"Tongue",			"TONGUE",		null,	newStringArray("Tongue"),			newStringArray("Tongue")),
		new DisplayableAnatomicConcept("44567001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-25000",	"Trachea",			"TRACHEA",		null,	newStringArray("Trachea"),			newStringArray("Trachea")),
		new DisplayableAnatomicConcept("65364008"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-73800",	"Ureter",			"URETER",		null,	newStringArray("Ureter"),			newStringArray("Ureter")),
		new DisplayableAnatomicConcept("13648007"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-75000",	"Urethra",			"URETHRA",		null,	newStringArray("Urethra"),			newStringArray("Urethra")),
		new DisplayableAnatomicConcept("35039007"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-83000",	"Uterus",			"UTERUS",		null,	newStringArray("Uterus"),			newStringArray("Uterus")),
		new DisplayableAnatomicConcept("76784001"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-82000",	"Vagina",			"VAGINA",		null,	newStringArray("Vagina"),			newStringArray("Vagina")),
		new DisplayableAnatomicConcept("45292006"/*This is SNOMED ID, not CUI*/,	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-81000",	"Vulva",			"VULVA",		null,	newStringArray("Vulva"),			newStringArray("Vulva")),
		new DisplayableAnatomicConcept("74670003"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15460",	"Wrist joint",		"WRIST",
			newStringArray(
				"Wrist",
				"muñeca"/*ES*/,
				"MUÒECA"/*ES ? misspelled*/,
				"pols"/*NL*/,
				"poignet"/*FR*/,
				"Handgelenk"/*DE*/,
				"καρπός"/*GR*/,
				"polso"/*IT,PT*/,
				"手首"/*JP*/,
				"손목"/*KR*/,
				"запястье руки"/*RU*/,
				"ranne"/*EE*/,
				"käe"/*EE*/
			), 
			newStringArray("Wrist joint"),		newStringArray("Wrist joint")),
		new DisplayableAnatomicConcept("51204001"/*This is SNOMED ID, not CUI*/,	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11167",	"Zygomatic arch",	"ZYGOMA",
			newStringArray("Zygoma"),
			newStringArray("Zygomatic arch"),	newStringArray("Zygomatic arch")),
	};
	
	protected static DictionaryOfConcepts anatomyConcepts = new DictionaryOfConcepts(anatomicConceptEntries,badAnatomyWords,"Anatomy");

	public static DictionaryOfConcepts getAnatomyConcepts() { return anatomyConcepts; }
		
	public static DisplayableAnatomicConcept findAnatomicConcept(AttributeList list) {
		// strategy is to look in specific attributes first, then general, and look in codes before free text ...
		DisplayableConcept anatomy = null;
		{
			CodedSequenceItem anatomicRegionSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.AnatomicRegionSequence);
			if (anatomicRegionSequence != null) {
//System.err.println("ProjectionXRayAnatomy.findAnatomicConcept(): anatomicRegionSequence = "+anatomicRegionSequence);
				anatomy = anatomyConcepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(anatomicRegionSequence);
if (anatomy != null) System.err.println("ProjectionXRayAnatomy.findAnatomicConcept(): found Anatomy in AnatomicRegionSequence = "+anatomy.toStringBrief());
			}
		}
		if (anatomy == null) {
			String bodyPartExamined = Attribute.getSingleStringValueOrNull(list,TagFromName.BodyPartExamined);
			if (bodyPartExamined != null) {
//System.err.println("ProjectionXRayAnatomy.findAnatomicConcept(): bodyPartExamined = "+bodyPartExamined);
				anatomy = anatomyConcepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(bodyPartExamined);
if (anatomy != null) System.err.println("ProjectionXRayAnatomy.findAnatomicConcept(): found Anatomy in BodyPartExamined = "+anatomy.toStringBrief());
			}
		}
		if (anatomy == null) {
			anatomy = findAmongstGeneralAttributes(list,anatomyConcepts,badLateralityOrViewOrAnatomyPhraseTriggers);
		}
		return (DisplayableAnatomicConcept)anatomy;
	}
	
	public static DisplayableViewConcept findView(AttributeList list) {
		// strategy is to look in specific attributes first, then general, and look in codes before free text ...
		DisplayableConcept view = null;
		{
			CodedSequenceItem viewCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ViewCodeSequence);
			if (viewCodeSequence != null) {
//System.err.println("ProjectionXRayAnatomy.findView(): viewCodeSequence = "+viewCodeSequence);
				view = viewPositionConcepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(viewCodeSequence);
if (view != null) System.err.println("ProjectionXRayAnatomy.findView(): found View in ViewCodeSequence = "+view.toStringBrief());
			}
		}
		if (view == null) {
			String viewPosition = Attribute.getSingleStringValueOrNull(list,TagFromName.ViewPosition);
			if (viewPosition != null) {
//System.err.println("ProjectionXRayAnatomy.findView(): bodyPartExamined = "+viewPosition);
				view = viewPositionConcepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(viewPosition);
if (view != null) System.err.println("ProjectionXRayAnatomy.findView(): found View in ViewPosition = "+view.toStringBrief());
			}
		}
		//if (view == null) {
		//	String bodyPartExamined = Attribute.getSingleStringValueOrNull(list,TagFromName.BodyPartExamined);		// view should NOT be encoded in BodyPartExamined, but sometimes is :(
		//	if (bodyPartExamined != null) {
//System.err.println("ProjectionXRayAnatomy.findAnatomicConcept(): bodyPartExamined = "+bodyPartExamined);
		//		view = viewPositionConcepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(bodyPartExamined);
//if (view != null) System.err.println("ProjectionXRayAnatomy.findView(): found in BodyPartExamined = "+view.toStringBrief());
		//	}
		//}
		if (view == null) {
			view = findAmongstGeneralAttributes(list,viewPositionConcepts,badLateralityOrViewOrAnatomyPhraseTriggers);
		}
		return (DisplayableViewConcept)view;
}

	public static DisplayableLateralityConcept findLaterality(AttributeList list,DisplayableAnatomicConcept anatomy) {
		// strategy is to look in specific attributes first, then check if unpaired anatomy prior to searching general attributes ...
		DisplayableConcept laterality = null;
		{
			String imageLaterality = Attribute.getSingleStringValueOrNull(list,TagFromName.ImageLaterality);
			if (imageLaterality != null) {
//System.err.println("ProjectionXRayAnatomy.findLaterality(): imageLaterality = "+imageLaterality);
				laterality = lateralityConcepts.findCodeStringExact(imageLaterality);
if (laterality != null) System.err.println("ProjectionXRayAnatomy.findLaterality(): found Laterality in ImageLaterality = "+laterality.toStringBrief());
			}
		}
		if (laterality == null) {
			String vLaterality = Attribute.getSingleStringValueOrNull(list,TagFromName.Laterality);
			if (vLaterality != null) {
//System.err.println("ProjectionXRayAnatomy.findLaterality(): imageLaterality = "+imageLaterality);
				laterality = lateralityConcepts.findCodeStringExact(vLaterality);
if (laterality != null) System.err.println("ProjectionXRayAnatomy.findLaterality(): found Laterality in Laterality = "+laterality.toStringBrief());
			}
		}
		if (laterality == null) {
			if (anatomy != null && !anatomy.isPairedStructure()) {
				laterality = lateralityConcepts.findCodeStringExact("U");
if (laterality != null) System.err.println("ProjectionXRayAnatomy.findLaterality(): anatomy is unpaired structure so use for Laterality = "+laterality.toStringBrief());
			}
		}
		if (laterality == null) {
			String viewPosition = Attribute.getSingleStringValueOrNull(list,TagFromName.ViewPosition);
			if (viewPosition != null) {
//System.err.println("ProjectionXRayAnatomy.findView(): bodyPartExamined = "+viewPosition);
				laterality = lateralityConcepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(viewPosition);
if (laterality != null) System.err.println("ProjectionXRayAnatomy.findLaterality(): found Laterality in ViewPosition = "+laterality.toStringBrief());
			}
		}
		if (laterality == null) {
			laterality = findAmongstGeneralAttributes(list,lateralityConcepts,badLateralityOrViewOrAnatomyPhraseTriggers);
		}
		return (DisplayableLateralityConcept)laterality;
}

	public static DisplayableConcept findAmongstGeneralAttributes(AttributeList list,DictionaryOfConcepts concepts,String[] badPhraseTriggers) {
		// strategy is to look in attributes of lower level entities first, and look in codes before free text ...
		DisplayableConcept found = null;
		{
			String imageComments = Attribute.getSingleStringValueOrNull(list,TagFromName.ImageComments);
			if (imageComments != null && !StringUtilities.containsRegardlessOfCase(imageComments,badPhraseTriggers)) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): imageComments = "+imageComments);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(imageComments);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in = ImageComments "+found.toStringBrief());
			}
		}
		if (found == null) {
			String protocolName = Attribute.getSingleStringValueOrNull(list,TagFromName.ProtocolName);
			if (protocolName != null && !StringUtilities.containsRegardlessOfCase(protocolName,badPhraseTriggers)) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): protocolName = "+seriesDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(protocolName);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in ProtocolName = "+found.toStringBrief());
			}
		}
		if (found == null) {
			String seriesDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesDescription);
			if (seriesDescription != null && !StringUtilities.containsRegardlessOfCase(seriesDescription,badPhraseTriggers)) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): seriesDescription = "+seriesDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(seriesDescription);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in SeriesDescription = "+found.toStringBrief());
			}
		}
		if (found == null) {
			CodedSequenceItem performedProtocolCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.PerformedProtocolCodeSequence);
			if (performedProtocolCodeSequence != null) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): performedProtocolCodeSequence = "+performedProtocolCodeSequence);
				found = concepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(performedProtocolCodeSequence);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in = PerformedProtocolCodeSequence "+found.toStringBrief());
			}
		}
		if (found == null) {
			String performedProcedureStepDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.PerformedProcedureStepDescription);
			if (performedProcedureStepDescription != null && !StringUtilities.containsRegardlessOfCase(performedProcedureStepDescription,badPhraseTriggers)) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): performedProcedureStepDescription = "+performedProcedureStepDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(performedProcedureStepDescription);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in = PerformedProcedureStepDescription "+found.toStringBrief());
			}
		}
		if (found == null) {
			CodedSequenceItem procedureCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ProcedureCodeSequence);
			if (procedureCodeSequence != null) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): procedureCodeSequence = "+procedureCodeSequence);
				found = concepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(procedureCodeSequence);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in = ProcedureCodeSequence "+found.toStringBrief());
			}
		}
		if (found == null) {
			String studyDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDescription);
			if (studyDescription != null && !StringUtilities.containsRegardlessOfCase(studyDescription,badPhraseTriggers)) {
//System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): seriesDescription = "+studyDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(studyDescription);
if (found != null) System.err.println("ProjectionXRayAnatomy.findAmongstGeneralAttributes(): found "+concepts.getDescriptionOfConcept()+" in = StudyDescription "+found.toStringBrief());
			}
		}
		return found;
	}
	

	/**
	 * <p>Read the DICOM input file and extract anatomical information.</p>
	 *
	 * @param	arg	array of one string, the filename to read
	 */
	public static void main(String arg[]) {
		if (arg.length == 1) {
			String inputFileName = arg[0];
			try {
				AttributeList list = new AttributeList();
				list.read(inputFileName);
				DisplayableAnatomicConcept anatomy = findAnatomicConcept(list);
				if (anatomy != null) {
					//System.err.print(anatomy);
				}
				else {
					System.err.println("########################### - ANATOMY NOT FOUND - ###########################");
				}
				DisplayableViewConcept view = findView(list);
				if (view != null) {
					//System.err.print(view);
				}
				else {
					System.err.println("########################### - VIEW NOT FOUND - ###########################");
				}
				DisplayableLateralityConcept laterality = findLaterality(list,anatomy);
				if (laterality != null) {
					//System.err.print(laterality);
				}
				else {
					System.err.println("########################### - LATERALITY NOT FOUND - ###########################");
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
}


	
