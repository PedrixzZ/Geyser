package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v361.BedrockUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.JavaItem;
import org.geysermc.connector.world.GlobalBlockPalette;

import java.io.InputStream;
import java.util.*;

public class Toolbox {

    static {
        InputStream stream = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/cached_palette.json");
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<LinkedHashMap<String, Object>> entries = new ArrayList<>();

        try {
            entries = mapper.readValue(stream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, BedrockItem> bedrockBlocks = new HashMap<>();
        Map<String, BedrockItem> bedrockItems = new HashMap<>();

        ByteBuf b = Unpooled.buffer();
        VarInts.writeUnsignedInt(b, entries.size());
        for (Map<String, Object> e : entries) {
            BedrockItem bedrockItem = new BedrockItem((String) e.get("name"), (int) e.get("id"), (int) e.get("data"));
            bedrockItems.put(bedrockItem.getIdentifier() + ":" + bedrockItem.getData(), bedrockItem);
            bedrockBlocks.put(bedrockItem.getIdentifier() + ":" + bedrockItem.getData(), bedrockItem);

            GlobalBlockPalette.registerMapping((int) e.get("id") << 4 | (int) e.get("data"));
            BedrockUtils.writeString(b, (String) e.get("name"));
            b.writeShortLE((int) e.get("data"));
            b.writeShortLE((int) e.get("id"));
        }

        CACHED_PALLETE = b;

        InputStream stream2 = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/items.json");
        if (stream2 == null) {
            throw new AssertionError("Items Table not found");
        }

        ObjectMapper mapper2 = new ObjectMapper();
        ArrayList<HashMap> s = new ArrayList<>();
        try {
            s = mapper2.readValue(stream2, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<StartGamePacket.ItemEntry> l = new ArrayList<>();
        for (HashMap e : s) {
            l.add(new StartGamePacket.ItemEntry((String) e.get("name"), (short) ((int) e.get("id"))));
            if (!bedrockItems.containsKey(e.get("name"))) {
                BedrockItem bedrockItem = new BedrockItem((String) e.get("name"), ((int) e.get("id")), 0);
                bedrockItems.put(bedrockItem.getIdentifier(), bedrockItem);
            }
        }

        ITEMS = l;

        BEDROCK_ITEMS = bedrockItems;

        InputStream javaItemStream = Toolbox.class.getClassLoader().getResourceAsStream("java/java_items.json");
        ObjectMapper javaItemMapper = new ObjectMapper();
        Map<String, HashMap> javaItemList = new HashMap<>();
        try {
            javaItemList = javaItemMapper.readValue(javaItemStream, new TypeReference<Map<String, HashMap>>(){});
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map<String, JavaItem> javaItems = new HashMap<String, JavaItem>();

        for (String str : javaItemList.keySet()) {
            javaItems.put(str, new JavaItem(str, (int) javaItemList.get(str).get("protocol_id")));
        }

        JAVA_ITEMS = javaItems;

        InputStream javaItemStream2 = Toolbox.class.getClassLoader().getResourceAsStream("java/java_blocks.json");
        ObjectMapper javaItemMapper2 = new ObjectMapper();
        Map<String, HashMap> javaItemList2 = new HashMap<>();
        try {
            javaItemList2 = javaItemMapper2.readValue(javaItemStream2, new TypeReference<Map<String, HashMap>>(){});
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map<String, JavaItem> javaBlocks = new HashMap<String, JavaItem>();

        for (String str : javaItemList2.keySet()) {
            javaBlocks.put(str, new JavaItem(str, (int) javaItemList2.get(str).get("protocol_id")));
        }

        JAVA_BLOCKS = javaBlocks;
        BEDROCK_BLOCKS = bedrockBlocks;

        GeyserLogger.DEFAULT.debug("Remapping items...");
        Remapper.ITEM_REMAPPER.registerConversions(bedrockItems, javaItems);
        GeyserLogger.DEFAULT.debug("Item remap complete!");

        GeyserLogger.DEFAULT.debug("Remapping blocks...");
        Remapper.BLOCK_REMAPPER.registerConversions(bedrockBlocks, javaBlocks);
        GeyserLogger.DEFAULT.debug("Block remap complete!");
    }

    public static final Collection<StartGamePacket.ItemEntry> ITEMS;

    public static final ByteBuf CACHED_PALLETE;

    public static final Map<String, BedrockItem> BEDROCK_ITEMS;
    public static final Map<String, JavaItem> JAVA_ITEMS;

    public static final Map<String, BedrockItem> BEDROCK_BLOCKS;
    public static final Map<String, JavaItem> JAVA_BLOCKS;

    //public static final byte[] EMPTY_CHUNK;
}