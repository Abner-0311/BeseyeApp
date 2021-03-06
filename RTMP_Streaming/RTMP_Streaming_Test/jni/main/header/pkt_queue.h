#include "libavformat/avformat.h"

#include <SDL.h>
#include <SDL_thread.h>

#ifndef DEF_PACKETQUEUE

typedef struct PacketQueue {
    AVPacketList *first_pkt, *last_pkt;
    int nb_packets;
    int size;
    int abort_request;
    SDL_mutex *mutex;
    SDL_cond *cond;
} PacketQueue;

static AVPacket flush_pkt;

#define DEF_PACKETQUEUE 1
#endif

void packet_queue_init(PacketQueue *q);
void packet_queue_destroy(PacketQueue *q);

void packet_queue_start(PacketQueue *q);
void packet_queue_flush(PacketQueue *q);
void packet_queue_abort(PacketQueue *q);

int packet_queue_get(PacketQueue *q, AVPacket *pkt, int block);
int packet_queue_put(PacketQueue *q, AVPacket *pkt);
