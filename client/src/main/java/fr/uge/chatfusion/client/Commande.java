package fr.uge.chatfusion.client;

public enum Commande {
    //********RFC******
    LOGIN_ANONYMOUS,      //0
    LOGIN_PASSWORD,       //1
    LOGIN_ACCEPTED,       //2
    LOGIN_REFUSED,        //3
    MESSAGE,              //4
    MESSAGE_PRIVATE,      //5
    FILE_PRIVATE, 	      //6
    FUSION_INIT,		  //7
    FUSION_INIT_OK, 	  //8
    FUSION_INIT_KO,	      //9
    FUSION_INIT_FWD,	  //10
    FUSION_REQUEST,		  //11
    FUSION_CHANGE_LEADER, //12
    FUSION_MERGE;		  //13
}