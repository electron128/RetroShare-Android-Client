

#include "bitdht/bdiface.h"
#include "bitdht/bdstddht.h"
#include "bdhandler.h"

#include <string.h>

//int main(int argc, char **argv)
int dowork(std::string bootstrapfile)
{

	/* startup dht : with a random id! */
        bdNodeId ownId;
        bdStdRandomNodeId(&ownId);

	uint16_t port = 6775;
	std::string appId = "bsId";
	//std::string bootstrapfile = "bdboot.txt";

	BitDhtHandler dht(&ownId, port, appId, bootstrapfile);

	/* install search node */
        bdNodeId searchId;

        //bdStdRandomNodeId(&searchId);

        // devz(devz) eca47627b7f11dc53d9dd437910124ac4d9d6559
        /* Unbenannt1 (01.11.2012 12:52:56)
           StartOffset: 00000000, EndeOffset: 00000013, Länge: 00000014 */

        unsigned char rawData[20] = {
        	0xEC, 0xA4, 0x76, 0x27, 0xB7, 0xF1, 0x1D, 0xC5, 0x3D, 0x9D, 0xD4, 0x37,
        	0x91, 0x01, 0x24, 0xAC, 0x4D, 0x9D, 0x65, 0x59
        };

        memcpy(searchId.data,rawData,BITDHT_KEY_LEN);

        std::cerr << "bssdht: searching for Id: ";
        bdStdPrintNodeId(std::cerr, &searchId);
	std::cerr << std::endl;

        dht.FindNode(&searchId);

	/* run your program */
	bdId resultId;
	uint32_t status;

        resultId.id = searchId;

	while(false == dht.SearchResult(&resultId, status))
	{
		sleep(10);
	}

        std::cerr << "bssdht: Found Result:" << std::endl;

        std::cerr << "\tId: ";
        bdStdPrintId(std::cerr, &resultId);
	std::cerr << std::endl;

	std::cerr << "\tstatus: " << status;
	std::cerr << std::endl;

	return 1;
}


		





