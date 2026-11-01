package com.tivibox.home;

import java.util.ArrayList;
import java.util.List;

/**
 * VTV channels for the Home row: the free live streams VTVgo publishes (vtvdigital.vn, HLS). If VTV moves a stream,
 * only this table needs to change.
 */
final class Channels {
    static final class Channel {
        final String id, name, url;

        Channel(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }

    private static final String SCTV = "https://vtvgolive-sctvdrm.vtvdigital.vn/manifest/";

    static final Channel[] ALL = {
            new Channel("vtv1", "VTV1", SCTV + "vtv1/master.m3u8"),
            new Channel("vtv2", "VTV2", SCTV + "vtv2/master.m3u8"),
            new Channel("vtv3", "VTV3", SCTV + "vtv3/master.m3u8"),
            new Channel("vtv4", "VTV4", SCTV + "vtv4/master.m3u8"),
            new Channel("vtv5", "VTV5", SCTV + "vtv5/master.m3u8"),
            new Channel("vtv5tnb", "VTV5 TNB", SCTV + "vtv5tnb/master.m3u8"),
            new Channel("vtv6", "VTV6", "https://vtvgolivepre-vtv02.vtvdigital.vn/hls/vtv/live/vtv6__vtv720p-h264/index.m3u8"),
            new Channel("vtv7", "VTV7", SCTV + "vtv7/master.m3u8"),
            new Channel("vtv8", "VTV8", SCTV + "vtv8/master.m3u8"),
            new Channel("vtv9", "VTV9", SCTV + "vtv9/master.m3u8"),
    };

    /** Shown by default: the nine national channels (VTV5 Tây Nam Bộ can be added in settings). */
    static final String DEFAULT = "vtv1,vtv2,vtv3,vtv4,vtv5,vtv6,vtv7,vtv8,vtv9";

    private Channels() {}

    static Channel byId(String id) {
        for (Channel c : ALL) if (c.id.equals(id)) return c;
        return null;
    }

    /** The chosen channels in table order. */
    static List<Channel> shown(Prefs prefs) {
        List<String> ids = prefs.channels();
        List<Channel> out = new ArrayList<>();
        for (Channel c : ALL) if (ids.contains(c.id)) out.add(c);
        return out;
    }
}
