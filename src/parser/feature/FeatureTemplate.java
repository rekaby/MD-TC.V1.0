package parser.feature;

import utils.Utils;

public class FeatureTemplate {
	
	/**
	 * "H"	: head
	 * "M"	: modifier
	 * "B"	: in-between tokens
	 * 
	 * "P"	: pos tag
	 * "W"	: word form or lemma
	 * "EMB": word embedding (word vector)
	 * 
	 * "p": previous token
	 * "n": next token
	 *
	 */
	
	public enum Arc {
		
		FEATURE_TEMPLATE_START,
	    
		/*************************************************
		 * Arc feature inspired by MST parser 
		 * ***********************************************/
		
	    // posL posIn posR
	    HP_BP_MP,			//CORE_POS_PC,	    
	    					//CORE_POS_XPC,
	    
	    // posL-1 posL posR posR+1
	    HPp_HP_MP_MPn,		//CORE_POS_PT0,
	    HP_MP_MPn,			//CORE_POS_PT1,
	    HPp_HP_MP,			//CORE_POS_PT2,
	    HPp_MP_MPn,			//CORE_POS_PT3,
	    HPp_HP_MPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MPp_MP,		//CORE_POS_APT0,
	    HP_MPp_MP,			//CORE_POS_APT1,
	    HP_HPn_MP,			//CORE_POS_APT2,
	    HPn_MPp_MP,			//CORE_POS_APT3,
	    HP_HPn_MPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MPp_MP,		//CORE_POS_BPT,
	    HP_HPn_MP_MPn,		//CORE_POS_CPT,

	    // unigram (form, lemma, pos, coarse_pos, morphology) 
	    CORE_HEAD_WORD,
	    CORE_HEAD_POS,
	    CORE_MOD_WORD,
	    CORE_MOD_POS,
	    CORE_HEAD_pWORD,
	    CORE_HEAD_nWORD,
	    CORE_MOD_pWORD,
	    CORE_MOD_nWORD,    
	    
	    // bigram  [word|lemma]-cross-[pos|cpos|mophlogy](-cross-distance)
	    HW_MW_HP_MP,			//CORE_BIGRAM_A,
	    MW_HP_MP,				//CORE_BIGRAM_B,
	    HW_HP_MP,				//CORE_BIGRAM_C,
	    MW_HP,					//CORE_BIGRAM_D,
	    HW_MP,					//CORE_BIGRAM_E,
	    HW_HP,					//CORE_BIGRAM_H,
	    MW_MP,					//CORE_BIGRAM_K,
	    HW_MW,					//CORE_BIGRAM_F,
	    HP_MP,					//CORE_BIGRAM_G,
	    
	    
	    // label feature
//	    CORE_LABEL_NTS1,
//	    CORE_LABEL_NTH,
//	    CORE_LABEL_NTI,
//	    CORE_LABEL_NTIA,
//	    CORE_LABEL_NTIB,
//	    CORE_LABEL_NTIC,
//	    CORE_LABEL_NTJ,
//	    HW_LABEL,
//	    HP_LABEL,
//	    MW_LABEL,
//	    MP_LABEL,
//	    //HW_MW_LABEL,
//	    HW_MP_LABEL,
//	    HP_MP_LABEL,
//	    MW_HP_LABEL,
//	    HP_pMP_LABEL,
//	    HP_nMP_LABEL,
//	    HP_pMW_LABEL,
//	    HP_nMW_LABEL,
//	    MP_pMP_LABEL,
//	    MP_nMP_LABEL,
//	    MP_pMW_LABEL,
//	    MP_nMW_LABEL,
	    
	    /*************************************************
		 * Context feature
		 * AG agent
		 * TH theme
		 * INS instrument  
		 * LOC location
		 * OWN Ownership
		 * PRO property
		 * NXT next to
		 * ***********************************************/
	    //Agent
	    HP_BP_MAGP,
	    
	    HPp_HP_MAGP_MAGPn,		//CORE_POS_PT0,
	    HP_MAGP_MAGPn,			//CORE_POS_PT1,
	    HPp_HP_MAGP,			//CORE_POS_PT2,
	    HPp_MAGP_MAGPn,			//CORE_POS_PT3,
	    HPp_HP_MAGPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MAGPp_MAGP,		//CORE_POS_APT0,
	    HP_MAGPp_MAGP,			//CORE_POS_APT1,
	    HP_HPn_MAGP,			//CORE_POS_APT2,
	    HPn_MAGPp_MAGP,			//CORE_POS_APT3,
	    HP_HPn_MAGPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MAGPp_MAGP,		//CORE_POS_BPT,
	    HP_HPn_MAGP_MAGPn,		//CORE_POS_CPT,
    
	    HW_MAGW_HP_MAGP,			//CORE_BIGRAM_A,
	    MAGW_HP_MAGP,				//CORE_BIGRAM_B,
	    HW_HP_MAGP,				//CORE_BIGRAM_C,
	    MAGW_HP,					//CORE_BIGRAM_D,
	    HW_MAGP,					//CORE_BIGRAM_E,
	    HW_MAGW,					//CORE_BIGRAM_F,
	    HP_MAGP,					//CORE_BIGRAM_G,
	
	    ////New Simple
	    HP_HPnAG,
	    MAGP_MAGPn,
	    HPp_HPAG,
	    MAGPp_MAGP,
	    HW_HWnAG,
	    MAGW_MAGWn,
	    HWp_HWAG,
	    MAGWp_MAGW,
	    //Newly introduced core features
	    HPAG,
	    HPnAG,
	    HPpAG,
	    MPAG,
	    MPnAG,
	    MPpAG,
	    HWAG,
	    HWnAG,
	    HWpAG,
	    MWAG,
	    MWnAG,
	    MWpAG,
	    
	    
	  //InferedAgent IAG
	    HP_BP_MIAGP,
	    
	    HPp_HP_MIAGP_MIAGPn,		//CORE_POS_PT0,
	    HP_MIAGP_MIAGPn,			//CORE_POS_PT1,
	    HPp_HP_MIAGP,			//CORE_POS_PT2,
	    HPp_MIAGP_MIAGPn,			//CORE_POS_PT3,
	    HPp_HP_MIAGPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MIAGPp_MIAGP,		//CORE_POS_APT0,
	    HP_MIAGPp_MIAGP,			//CORE_POS_APT1,
	    HP_HPn_MIAGP,			//CORE_POS_APT2,
	    HPn_MIAGPp_MIAGP,			//CORE_POS_APT3,
	    HP_HPn_MIAGPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MIAGPp_MIAGP,		//CORE_POS_BPT,
	    HP_HPn_MIAGP_MIAGPn,		//CORE_POS_CPT,
    
	    HW_MIAGW_HP_MIAGP,			//CORE_BIGRAM_A,
	    MIAGW_HP_MIAGP,				//CORE_BIGRAM_B,
	    HW_HP_MIAGP,				//CORE_BIGRAM_C,
	    MIAGW_HP,					//CORE_BIGRAM_D,
	    HW_MIAGP,					//CORE_BIGRAM_E,
	    HW_MIAGW,					//CORE_BIGRAM_F,
	    HP_MIAGP,					//CORE_BIGRAM_G,
	
	    ////New Simple
	    HP_HPnIAG,
	    MIAGP_MIAGPn,
	    HPp_HPIAG,
	    MIAGPp_MIAGP,
	    HW_HWnIAG,
	    MIAGW_MIAGWn,
	    HWp_HWIAG,
	    MIAGWp_MIAGW,
	  //Newly introduced core features
	    HPIAG,
	    HPnIAG,
	    HPpIAG,
	    MPIAG,
	    MPnIAG,
	    MPpIAG,
	    HWIAG,
	    HWnIAG,
	    HWpIAG,
	    MWIAG,
	    MWnIAG,
	    MWpIAG,

	    
	    
	    
	    //Theme
	    HP_BP_MTHP,
	    
	    HPp_HP_MTHP_MTHPn,		//CORE_POS_PT0,
	    HP_MTHP_MTHPn,			//CORE_POS_PT1,
	    HPp_HP_MTHP,			//CORE_POS_PT2,
	    HPp_MTHP_MTHPn,			//CORE_POS_PT3,
	    HPp_HP_MTHPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MTHPp_MTHP,		//CORE_POS_APT0,
	    HP_MTHPp_MTHP,			//CORE_POS_APT1,
	    HP_HPn_MTHP,			//CORE_POS_APT2,
	    HPn_MTHPp_MTHP,			//CORE_POS_APT3,
	    HP_HPn_MTHPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MTHPp_MTHP,		//CORE_POS_BPT,
	    HP_HPn_MTHP_MTHPn,		//CORE_POS_CPT,
    
	    HW_MTHW_HP_MTHP,			//CORE_BIGRAM_A,
	    MTHW_HP_MTHP,				//CORE_BIGRAM_B,
	    HW_HP_MTHP,				//CORE_BIGRAM_C,
	    MTHW_HP,					//CORE_BIGRAM_D,
	    HW_MTHP,					//CORE_BIGRAM_E,
	    HW_MTHW,					//CORE_BIGRAM_F,
	    HP_MTHP,					//CORE_BIGRAM_G,
	  
	    
	    ////New Simple
	    HP_HPnTH,
	    MTHP_MTHPn,
	    HPp_HPTH,
	    MTHPp_MTHP,
	    HW_HWnTH,
	    MTHW_MTHWn,
	    HWp_HWTH,
	    MTHWp_MTHW,
	  //Newly introduced core features
	    HPTH,
	    HPnTH,
	    HPpTH,
	    MPTH,
	    MPnTH,
	    MPpTH,
	    HWTH,
	    HWnTH,
	    HWpTH,
	    MWTH,
	    MWnTH,
	    MWpTH,

	
	  //Theme ITH
	    HP_BP_MITHP,
	    
	    HPp_HP_MITHP_MITHPn,		//CORE_POS_PT0,
	    HP_MITHP_MITHPn,			//CORE_POS_PT1,
	    HPp_HP_MITHP,			//CORE_POS_PT2,
	    HPp_MITHP_MITHPn,			//CORE_POS_PT3,
	    HPp_HP_MITHPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MITHPp_MITHP,		//CORE_POS_APT0,
	    HP_MITHPp_MITHP,			//CORE_POS_APT1,
	    HP_HPn_MITHP,			//CORE_POS_APT2,
	    HPn_MITHPp_MITHP,			//CORE_POS_APT3,
	    HP_HPn_MITHPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MITHPp_MITHP,		//CORE_POS_BPT,
	    HP_HPn_MITHP_MITHPn,		//CORE_POS_CPT,
    
	    HW_MITHW_HP_MITHP,			//CORE_BIGRAM_A,
	    MITHW_HP_MITHP,				//CORE_BIGRAM_B,
	    HW_HP_MITHP,				//CORE_BIGRAM_C,
	    MITHW_HP,					//CORE_BIGRAM_D,
	    HW_MITHP,					//CORE_BIGRAM_E,
	    HW_MITHW,					//CORE_BIGRAM_F,
	    HP_MITHP,					//CORE_BIGRAM_G,
	  
	    
	    ////New Simple
	    HP_HPnITH,
	    MITHP_MITHPn,
	    HPp_HPITH,
	    MITHPp_MITHP,
	    HW_HWnITH,
	    MITHW_MITHWn,
	    HWp_HWITH,
	    MITHWp_MITHW,
	  //Newly introduced core features
	    HPITH,
	    HPnITH,
	    HPpITH,
	    MPITH,
	    MPnITH,
	    MPpITH,
	    HWITH,
	    HWnITH,
	    HWpITH,
	    MWITH,
	    MWnITH,
	    MWpITH,

	    
	    //Instrument
	    HP_BP_MINSP,
	    
	    HPp_HP_MINSP_MINSPn,		//CORE_POS_PT0,
	    HP_MINSP_MINSPn,			//CORE_POS_PT1,
	    HPp_HP_MINSP,			//CORE_POS_PT2,
	    HPp_MINSP_MINSPn,			//CORE_POS_PT3,
	    HPp_HP_MINSPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MINSPp_MINSP,		//CORE_POS_APT0,
	    HP_MINSPp_MINSP,			//CORE_POS_APT1,
	    HP_HPn_MINSP,			//CORE_POS_APT2,
	    HPn_MINSPp_MINSP,			//CORE_POS_APT3,
	    HP_HPn_MINSPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MINSPp_MINSP,		//CORE_POS_BPT,
	    HP_HPn_MINSP_MINSPn,		//CORE_POS_CPT,
    
	    HW_MINSW_HP_MINSP,			//CORE_BIGRAM_A,
	    MINSW_HP_MINSP,				//CORE_BIGRAM_B,
	    HW_HP_MINSP,				//CORE_BIGRAM_C,
	    MINSW_HP,					//CORE_BIGRAM_D,
	    HW_MINSP,					//CORE_BIGRAM_E,
	    HW_MINSW,					//CORE_BIGRAM_F,
	    HP_MINSP,					//CORE_BIGRAM_G,
	    
	  //Ownership
	    HP_BP_MOWNP,
	    
	    HPp_HP_MOWNP_MOWNPn,		//CORE_POS_PT0,
	    HP_MOWNP_MOWNPn,			//CORE_POS_PT1,
	    HPp_HP_MOWNP,			//CORE_POS_PT2,
	    HPp_MOWNP_MOWNPn,			//CORE_POS_PT3,
	    HPp_HP_MOWNPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MOWNPp_MOWNP,		//CORE_POS_APT0,
	    HP_MOWNPp_MOWNP,			//CORE_POS_APT1,
	    HP_HPn_MOWNP,			//CORE_POS_APT2,
	    HPn_MOWNPp_MOWNP,			//CORE_POS_APT3,
	    HP_HPn_MOWNPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MOWNPp_MOWNP,		//CORE_POS_BPT,
	    HP_HPn_MOWNP_MOWNPn,		//CORE_POS_CPT,
    
	    HW_MOWNW_HP_MOWNP,			//CORE_BIGRAM_A,
	    MOWNW_HP_MOWNP,				//CORE_BIGRAM_B,
	    HW_HP_MOWNP,				//CORE_BIGRAM_C,
	    MOWNW_HP,					//CORE_BIGRAM_D,
	    HW_MOWNP,					//CORE_BIGRAM_E,
	    HW_MOWNW,					//CORE_BIGRAM_F,
	    HP_MOWNP,					//CORE_BIGRAM_G,
	    
	    
	////New Simple
	    HP_HPnOWN,
	    MOWNP_MOWNPn,
	    HPp_HPOWN,
	    MOWNPp_MOWNP,
	    HW_HWnOWN,
	    MOWNW_MOWNWn,
	    HWp_HWOWN,
	    MOWNWp_MOWNW,
	    
	  //Newly introduced core features
	    HPOWN,
	    HPnOWN,
	    HPpOWN,
	    MPOWN,
	    MPnOWN,
	    MPpOWN,
	    HWOWN,
	    HWnOWN,
	    HWpOWN,
	    MWOWN,
	    MWnOWN,
	    MWpOWN,
 
	    //Location
	    HP_BP_MLOCP,
	    
	    HPp_HP_MLOCP_MLOCPn,		//CORE_POS_PT0,
	    HP_MLOCP_MLOCPn,			//CORE_POS_PT1,
	    HPp_HP_MLOCP,			//CORE_POS_PT2,
	    HPp_MLOCP_MLOCPn,			//CORE_POS_PT3,
	    HPp_HP_MLOCPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MLOCPp_MLOCP,		//CORE_POS_APT0,
	    HP_MLOCPp_MLOCP,			//CORE_POS_APT1,
	    HP_HPn_MLOCP,			//CORE_POS_APT2,
	    HPn_MLOCPp_MLOCP,			//CORE_POS_APT3,
	    HP_HPn_MLOCPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MLOCPp_MLOCP,		//CORE_POS_BPT,
	    HP_HPn_MLOCP_MLOCPn,		//CORE_POS_CPT,
    
	    HW_MLOCW_HP_MLOCP,			//CORE_BIGRAM_A,
	    MLOCW_HP_MLOCP,				//CORE_BIGRAM_B,
	    HW_HP_MLOCP,				//CORE_BIGRAM_C,
	    MLOCW_HP,					//CORE_BIGRAM_D,
	    HW_MLOCP,					//CORE_BIGRAM_E,
	    HW_MLOCW,					//CORE_BIGRAM_F,
	    HP_MLOCP,					//CORE_BIGRAM_G,
	
	////New Simple
	    HP_HPnLOC,
	    MLOCP_MLOCPn,
	    HPp_HPLOC,
	    MLOCPp_MLOCP,
	    HW_HWnLOC,
	    MLOCW_MLOCWn,
	    HWp_HWLOC,
	    MLOCWp_MLOCW,
	    
	  //Newly introduced core features
	    HPLOC,
	    HPnLOC,
	    HPpLOC,
	    MPLOC,
	    MPnLOC,
	    MPpLOC,
	    HWLOC,
	    HWnLOC,
	    HWpLOC,
	    MWLOC,
	    MWnLOC,
	    MWpLOC,

	    
	    //PROperty
	    HP_BP_MPROP,
	    
	    HPp_HP_MPROP_MPROPn,		//CORE_POS_PT0,
	    HP_MPROP_MPROPn,			//CORE_POS_PT1,
	    HPp_HP_MPROP,			//CORE_POS_PT2,
	    HPp_MPROP_MPROPn,			//CORE_POS_PT3,
	    HPp_HP_MPROPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MPROPp_MPROP,		//CORE_POS_APT0,
	    HP_MPROPp_MPROP,			//CORE_POS_APT1,
	    HP_HPn_MPROP,			//CORE_POS_APT2,
	    HPn_MPROPp_MPROP,			//CORE_POS_APT3,
	    HP_HPn_MPROPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MPROPp_MPROP,		//CORE_POS_BPT,
	    HP_HPn_MPROP_MPROPn,		//CORE_POS_CPT,
    
	    HW_MPROW_HP_MPROP,			//CORE_BIGRAM_A,
	    MPROW_HP_MPROP,				//CORE_BIGRAM_B,
	    HW_HP_MPROP,				//CORE_BIGRAM_C,
	    MPROW_HP,					//CORE_BIGRAM_D,
	    HW_MPROP,					//CORE_BIGRAM_E,
	    HW_MPROW,					//CORE_BIGRAM_F,
	    HP_MPROP,					//CORE_BIGRAM_G,
	
	    
	////New Simple
	    HP_HPnPRO,
	    MPROP_MPROPn,
	    HPp_HPPRO,
	    MPROPp_MPROP,
	    HW_HWnPRO,
	    MPROW_MPROWn,
	    HWp_HWPRO,
	    MPROWp_MPROW,
	    
	  //Newly introduced core features
	    HPPRO,
	    HPnPRO,
	    HPpPRO,
	    MPPRO,
	    MPnPRO,
	    MPpPRO,
	    HWPRO,
	    HWnPRO,
	    HWpPRO,
	    MWPRO,
	    MWnPRO,
	    MWpPRO,

	    //NeXT to
	    HP_BP_MNXTP,
	    
	    HPp_HP_MNXTP_MNXTPn,		//CORE_POS_PT0,
	    HP_MNXTP_MNXTPn,			//CORE_POS_PT1,
	    HPp_HP_MNXTP,			//CORE_POS_PT2,
	    HPp_MNXTP_MNXTPn,			//CORE_POS_PT3,
	    HPp_HP_MNXTPn,			//CORE_POS_PT4,
    
	    // posL posL+1 posR-1 posR
	    HP_HPn_MNXTPp_MNXTP,		//CORE_POS_APT0,
	    HP_MNXTPp_MNXTP,			//CORE_POS_APT1,
	    HP_HPn_MNXTP,			//CORE_POS_APT2,
	    HPn_MNXTPp_MNXTP,			//CORE_POS_APT3,
	    HP_HPn_MNXTPp,			//CORE_POS_APT4,
	    
	    // posL-1 posL posR-1 posR
	    // posL posL+1 posR posR+1
	    HPp_HP_MNXTPp_MNXTP,		//CORE_POS_BPT,
	    HP_HPn_MNXTP_MNXTPn,		//CORE_POS_CPT,
    
	    HW_MNXTW_HP_MNXTP,			//CORE_BIGRAM_A,
	    MNXTW_HP_MNXTP,				//CORE_BIGRAM_B,
	    HW_HP_MNXTP,				//CORE_BIGRAM_C,
	    MNXTW_HP,					//CORE_BIGRAM_D,
	    HW_MNXTP,					//CORE_BIGRAM_E,
	    HW_MNXTW,					//CORE_BIGRAM_F,
	    HP_MNXTP,					//CORE_BIGRAM_G,
	
	    
	////New Simple
	    HP_HPnNXT,
	    MNXTP_MNXTPn,
	    HPp_HPNXT,
	    MNXTPp_MNXTP,
	    HW_HWnNXT,
	    MNXTW_MNXTWn,
	    HWp_HWNXT,
	    MNXTWp_MNXTW,
	    
	  //Newly introduced core features
	    HPNXT,
	    HPnNXT,
	    HPpNXT,
	    MPNXT,
	    MPnNXT,
	    MPpNXT,
	    HWNXT,
	    HWnNXT,
	    HWpNXT,
	    MWNXT,
	    MWnNXT,
	    MWpNXT,

	    /*************************************************
		 * 2o feature  
		 * ***********************************************/

	    HP_SP_MP,
		HC_SC_MC,

		pHC_HC_SC_MC,
		HC_nHC_SC_MC,
		HC_pSC_SC_MC,
		HC_SC_nSC_MC,
		HC_SC_pMC_MC,
		HC_SC_MC_nMC,

		pHC_HL_SC_MC,
		HL_nHC_SC_MC,
		HL_pSC_SC_MC,
		HL_SC_nSC_MC,
		HL_SC_pMC_MC,
		HL_SC_MC_nMC,

		pHC_HC_SL_MC,
		HC_nHC_SL_MC,
		HC_pSC_SL_MC,
		HC_SL_nSC_MC,
		HC_SL_pMC_MC,
		HC_SL_MC_nMC,

		pHC_HC_SC_ML,
		HC_nHC_SC_ML,
		HC_pSC_SC_ML,
		HC_SC_nSC_ML,
		HC_SC_pMC_ML,
		HC_SC_ML_nMC,

		HC_MC_SC_pHC_pMC,
		HC_MC_SC_pHC_pSC,
		HC_MC_SC_pMC_pSC,
		HC_MC_SC_nHC_nMC,
		HC_MC_SC_nHC_nSC,
		HC_MC_SC_nMC_nSC,
		HC_MC_SC_pHC_nMC,
		HC_MC_SC_pHC_nSC,
		HC_MC_SC_pMC_nSC,
		HC_MC_SC_nHC_pMC,
		HC_MC_SC_nHC_pSC,
		HC_MC_SC_nMC_pSC,

		SP_MP,
		SW_MW,
		SW_MP,
		SP_MW,
		SC_MC,
		SL_ML,
		SL_MC,
		SC_ML,

		// head bigram
		H1P_H2P_M1P_M2P,
		H1P_H2P_M1P_M2P_DIR,
		H1C_H2C_M1C_M2C,
		H1C_H2C_M1C_M2C_DIR,

		// gp-p-c
		GP_HP_MP,
		GC_HC_MC,
		GL_HC_MC,
		GC_HL_MC,
		GC_HC_ML,

		GL_HL_MC,
		GL_HC_ML,
		GC_HL_ML,
		GL_HL_ML,

		GC_HC,
		GL_HC,
		GC_HL,
		GL_HL,

		GC_MC,	// this block only cross with dir flag
		GL_MC,
		GC_ML,
		GL_ML,
		HC_MC,
		HL_MC,
		HC_ML,
		HL_ML,

		pGC_GC_HC_MC,
		GC_nGC_HC_MC,
		GC_pHC_HC_MC,
		GC_HC_nHC_MC,
		GC_HC_pMC_MC,
		GC_HC_MC_nMC,

		pGC_GL_HC_MC,
		GL_nGC_HC_MC,
		GL_pHC_HC_MC,
		GL_HC_nHC_MC,
		GL_HC_pMC_MC,
		GL_HC_MC_nMC,

		pGC_GC_HL_MC,
		GC_nGC_HL_MC,
		GC_pHC_HL_MC,
		GC_HL_nHC_MC,
		GC_HL_pMC_MC,
		GC_HL_MC_nMC,

		pGC_GC_HC_ML,
		GC_nGC_HC_ML,
		GC_pHC_HC_ML,
		GC_HC_nHC_ML,
		GC_HC_pMC_ML,
		GC_HC_ML_nMC,

		GC_HC_MC_pGC_pHC,
		GC_HC_MC_pGC_pMC,
		GC_HC_MC_pHC_pMC,
		GC_HC_MC_nGC_nHC,
		GC_HC_MC_nGC_nMC,
		GC_HC_MC_nHC_nMC,
		GC_HC_MC_pGC_nHC,
		GC_HC_MC_pGC_nMC,
		GC_HC_MC_pHC_nMC,
		GC_HC_MC_nGC_pHC,
		GC_HC_MC_nGC_pMC,
		GC_HC_MC_nHC_pMC,

		// gp sibling
		GC_HC_MC_SC,
		GL_HC_MC_SC,
		GC_HL_MC_SC,
		GC_HC_ML_SC,
		GC_HC_MC_SL,

		// tri-sibling
		HC_PC_MC_NC,
		HL_PC_MC_NC,
		HC_PL_MC_NC,
		HC_PC_ML_NC,
		HC_PC_MC_NL,

		HC_PC_NC,
		PC_MC_NC,
		HL_PC_NC,
		HC_PL_NC,
		HC_PC_NL,
		PL_MC_NC,
		PC_ML_NC,
		PC_MC_NL,

		PC_NC,
		PL_NC,
		PC_NL,

		// ggpc
		GGC_GC_HC_MC,
		GGL_GC_HC_MC,
		GGC_GL_HC_MC,
		GGC_GC_HL_MC,
		GGC_GC_HC_ML,

		GGC_HC_MC,
		GGL_HC_MC,
		GGC_HL_MC,
		GGC_HC_ML,
		GGC_GC_MC,
		GGL_GC_MC,
		GGC_GL_MC,
		GGC_GC_ML,
		GGC_MC,
		GGL_MC,
		GGC_ML,
		GGL_ML,

		// psc
		HC_MC_CC_SC,
		HL_MC_CC_SC,
		HC_ML_CC_SC,
		HC_MC_CL_SC,
		HC_MC_CC_SL,

		HC_CC_SC,
		HL_CC_SC,
		HC_CL_SC,
		HC_CC_SL,

		// pp attachment
		PP_HC_MC,
		PP_HL_MC,
		PP_HC_ML,
		PP_HL_ML,

		PP_PL_HC_MC,
		PP_PL_HL_MC,
		PP_PL_HC_ML,
		PP_PL_HL_ML,

		// conjunction
		CC_CP_LP_RP,
		CC_CP_LC_RC,
		CC_CW_LP_RP,
		CC_CW_LC_RC,

		CC_LC_RC_FID,

		CC_CP_HC_AC,
		CC_CP_HL_AL,
		CC_CW_HC_AC,
		CC_CW_HL_AL,

		// PNX
		PNX_MW,
		PNX_HP_MW,

		// right branch
		RB,

		// child num
		CN_HP_NUM,
		CN_HL_NUM,
		CN_HP_LNUM_RNUM,
		CN_STR,

		// heavy
		HV_HP,
		HV_HC,

		// neighbor
		NB_HP_LC_RC,
		NB_HC_LC_RC,
		NB_HL_LC_RC,
		NB_GC_HC_LC_RC,
		NB_GC_HL_LC_RC,
		NB_GL_HC_LC_RC,

		// non-proj
		NP,
		NP_MC,
		NP_HC,
		NP_HL,
		NP_ML,
		NP_HC_MC,
		NP_HL_MC,
		NP_HC_ML,
		NP_HL_ML,

		/*************************************************
		 * word embedding feature  
		 * ***********************************************/
	    
	    HEAD_EMB,
	    MOD_EMB,
	    
	    
	    FEATURE_TEMPLATE_END;
		
		public final static int numArcFeatBits = Utils.log2(FEATURE_TEMPLATE_END.ordinal());
	}
	/**
	 * "H"	: head
	 * "M"	: modifier
	 * "B"	: in-between tokens
	 * 
	 * "P"	: pos tag
	 * "W"	: word form or lemma
	 * "EMB": word embedding (word vector)
	 * 
	 * "p": previous token
	 * "n": next token
	 *
	 */
	public enum Word {

		FEATURE_TEMPLATE_START,
		
		/*************************************************
		 * Word features for matrix/tensor 
		 * AG agent
		 * TH theme
		 * INS instrument
		 * VB verb
		 * ***********************************************/
		
		WORDFV_BIAS,
		
	    WORDFV_W0,
	    WORDFV_Wp,
	    WORDFV_Wn,
	    WORDFV_W0P0,
	    
	    WORDFV_F,
	    WORDFV_L0F,
	    WORDFV_P0F,
	    
	    WORDFV_P0,
	    WORDFV_Pp,
	    WORDFV_Pn,
	    WORDFV_PpP0,
	    WORDFV_P0Pn,
	    WORDFV_PpP0Pn,//TODO Question
	    
	    WORDFV_EMB,
	    
	    //Unigram
	    WORDFV_AG0,
	    WORDFV_TH0,
	    WORDFV_INS0,
	    WORDFV_VB0,
	    WORDFV_OWNER0,
	    WORDFV_OWNED0,

	    //Bigram
	    WORDFV_AGpAG0,
	    WORDFV_THpAG0,
	    WORDFV_VBpAG0,
	    
	    WORDFV_AG0AGn,
	    WORDFV_AG0THn,
	    WORDFV_AG0VBn,
	    
	    WORDFV_AGpTH0,
	    WORDFV_THpTH0,
	    WORDFV_VBpTH0,
	    
	    WORDFV_TH0AGn,
	    WORDFV_TH0THn,
	    WORDFV_TH0VBn,
	    
	    FEATURE_TEMPLATE_END;
	    
		public final static int numWordFeatBits = Utils.log2(FEATURE_TEMPLATE_END.ordinal());
	}
}


