package de.canitzp.carz.client;


import com.sun.imageio.plugins.common.ImageUtil;
import de.canitzp.carz.util.PlayerUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldServer;
import org.lwjgl.opengl.GL11;
import sun.nio.ch.IOUtil;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author canitzp
 */
@SuppressWarnings("WeakerAccess")
public class PixelMesh {

    public static final UUID EMPTY_UUID = new UUID(0, 0);
    public static final UUID INTERNAL_UUID = new UUID(0, 1);

    private Pixel[][] pixels;
    private String name;
    private UUID id, owner = EMPTY_UUID;
    private int offsetX = 0, offsetY = 0;
    private int glTextureId = -1;

    public PixelMesh(String name, int size, UUID id, UUID owner) {
        this.name = name;
        this.id = id;
        this.owner = owner;
        this.pixels = new Pixel[size][size];
        for(int i = 0; i < this.pixels.length; i++){
            this.pixels[i] = getUniqueFilledPixelArray(size);
        }
    }

    public PixelMesh(String name, int size, UUID owner) {
        this(name, size, UUID.randomUUID(), owner);
    }

    public PixelMesh(String name, PacketBuffer buf) {
        this.name = name;
        this.fromBytes(buf);
    }

    private Pixel[] getUniqueFilledPixelArray(int size){
        Pixel[] empty = new Pixel[size];
        Arrays.fill(empty, Pixel.EMPTY);
        return empty;
    }

    public Pixel[][] getPixels() {
        return pixels;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nonnull UUID owner) {
        this.owner = owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    private boolean isInRange(int line) {
        return line >= this.pixels.length;
    }

    private void checkRange(int line) {
        if (isInRange(line)) {
            throw new RuntimeException("You're trying to get a line of a PixelMesh that hasn't the needed size! You want: " + line + "  We have as maximum: " + (this.pixels.length - 1));
        }
    }

    public Pixel[] getLine(int line) {
        checkRange(line);
        return this.pixels[line];
    }

    public PixelMesh setLine(int line, Pixel[] pixels) {
        checkRange(line);
        if (this.pixels.length != pixels.length) {
            throw new RuntimeException("You want to set a line of Pixels to a PixelMesh, but the sizes are different! Your size: " + pixels.length + "  Size of the Mesh: " + this.pixels.length);
        }
        this.pixels[line] = pixels;
        this.setChanged();
        return this;
    }

    public PixelMesh setPixel(int x, int y, Pixel pixel){
        this.getPixels()[x][y] = pixel;
        this.setChanged();
        return this;
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void offset(int x, int y){
        this.offsetX += x;
        this.offsetY += y;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    @Override
    public String toString() {
        return "PixelMesh{" + this.getName() + " " + this.pixels.length + " " + this.getOwner() + "}";
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUniqueId(this.id);
        buf.writeUniqueId(this.owner);
        buf.writeInt(this.pixels.length);
        buf.writeInt(this.offsetX);
        buf.writeInt(this.offsetY);
        for (Pixel[] px : this.pixels) {
            for (Pixel p : px) {
                p.toBytes(buf);
            }
        }
    }

    public void fromBytes(PacketBuffer buf) {
        this.id = buf.readUniqueId();
        this.owner = buf.readUniqueId();
        int amount = buf.readInt();
        this.offsetX = buf.readInt();
        this.offsetY = buf.readInt();
        this.pixels = new Pixel[amount][amount];
        for (int i = 0; i < amount; i++) {
            for (int j = 0; j < amount; j++) {
                this.pixels[i][j] = new Pixel(buf);
            }
        }
    }

    public void render(int x, int y){
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Pixel[][] pixels1 = this.pixels;
        for (int row = 0; row < pixels1.length; row++) {
            Pixel[] pixels = pixels1[row];
            for (int column = 0; column < pixels.length; column++) {
                Pixel pixel = pixels[column];
                if(pixel.isValid()){
                    pixel.render(x + column, y + row);
                }
            }
        }
        GlStateManager.enableTexture2D();
        //RenderHelper.enableStandardItemLighting();
    }

    @SuppressWarnings("ConstantConditions")
    public boolean canBeEditedBy(EntityPlayer player){
        return player.isCreative() || PlayerUtil.isOperator(player) || player.getGameProfile().getId().equals(this.owner);
    }

    public PixelMesh setChanged(){
        this.glTextureId = -1;
        return this;
    }

    public void bindTexture(){
        if(glTextureId == -1){
            BufferedImage meshImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Pixel[][] pixels1 = this.getPixels();
            for (int row = 0; row < pixels1.length; row++) {
                Pixel[] pixels = pixels1[row];
                for (int column = 0; column < pixels.length; column++) {
                    Pixel pixel = pixels[column];
                    if(pixel.isValid()){
                        meshImage.setRGB(column, row, pixel.toHex() | 0xFF000000); // disable the alpha if any exists
                    } else {
                        meshImage.setRGB(column, row, 0x00FFFFFF); // alpha
                    }
                }
            }
            this.glTextureId = TextureUtil.glGenTextures();
            TextureUtil.uploadTextureImage(this.glTextureId, meshImage);
        } else {
            GlStateManager.bindTexture(this.glTextureId);
        }
    }

}
