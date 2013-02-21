LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc

LOCAL_MODULE    := bitdht
LOCAL_SRC_FILES := \
	com_example_retroshare_remote_bitdht.cc	\
	example/bdhandler.cc	\
	example/bssdht.cc	\
	example/bootstrap_fn.cc	\
	bitdht/bencode.c	\
	bitdht/bdobj.cc    	\
	bitdht/bdmsgs.cc	\
	bitdht/bdpeer.cc	\
	bitdht/bdquery.cc	\
	bitdht/bdhash.cc	\
	bitdht/bdstore.cc	\
	bitdht/bdnode.cc	\
	bitdht/bdmanager.cc	\
	bitdht/bdstddht.cc	\
	bitdht/bdhistory.cc	\
	util/bdnet.cc 	 	\
	util/bdthreads.cc  	\
	util/bdrandom.cc  	\
	util/bdstring.cc	\
	udp/udplayer.cc		\
	udp/udpstack.cc		\
	udp/udpbitdht.cc  	\
	bitdht/bdconnection.cc	\
	bitdht/bdfilter.cc	\
	bitdht/bdaccount.cc	\
	bitdht/bdquerymgr.cc	\
	util/bdbloom.cc		\
	bitdht/bdfriendlist.cc	\

include $(BUILD_SHARED_LIBRARY)