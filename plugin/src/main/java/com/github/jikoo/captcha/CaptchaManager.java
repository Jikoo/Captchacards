package com.github.jikoo.captcha;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.HashReplacement;
import com.github.jikoo.captcha.util.lang.ItemNameReplacement;
import com.github.jikoo.captcha.util.lang.Messages;
import com.github.jikoo.captcha.util.lang.QuantityReplacement;
import com.github.jikoo.captcha.util.lang.SimpleReplacement;
import com.github.jikoo.planarwrappers.lang.Replacement;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CaptchaManager {

  public static final @NotNull NamespacedKey KEY_UNCAPTCHA_RECIPE =
      Objects.requireNonNull(NamespacedKey.fromString("captcha:craft_open"));
  public static final @NotNull NamespacedKey KEY_SKIP_CONVERT =
      Objects.requireNonNull(NamespacedKey.fromString("captcha:skip_convert"));
  public static final @NotNull NamespacedKey KEY_BLANK =
      Objects.requireNonNull(NamespacedKey.fromString("captcha:blank"));
  public static final @NotNull NamespacedKey KEY_HASH =
      Objects.requireNonNull(NamespacedKey.fromString("captcha:hash"));
  private static final int MAX_CAPTCHA_DEPTH = 2;

  private final LoadingCache<String, ItemStack> cache =
      Caffeine.newBuilder()
          .maximumSize(500L)
          .expireAfterAccess(Duration.ofMinutes(30))
          .evictionListener(
              (RemovalListener<String, ItemStack>) (key, value, cause) -> {
                if (key != null && value != null) {
                  save(key, value);
                }
              }
          )
          .build(
              new CacheLoader<>() {
                @Override
                public @NotNull ItemStack load(@NotNull String hash) throws Exception {
                  Path hashPath = dataDir.resolve(hash + ".nbt");
                  if (!Files.exists(hashPath)) {
                    throw new FileNotFoundException();
                  }
                  try (InputStream in = Files.newInputStream(hashPath)) {
                    return ItemStack.deserializeBytes(in.readAllBytes());
                  }
                }
              }
          );
  private final @NotNull Path dataDir;
  private final @NotNull ComponentLangManager lang;
  private final @NotNull Logger logger;

  public CaptchaManager(@NotNull Path dataDir, @NotNull ComponentLangManager lang, @NotNull Logger logger) {
    this.dataDir = dataDir;
    this.lang = lang;
    this.logger = logger;
  }

  /**
   * Get the directory where captchacard contents will be serialized.
   *
   * @return the path of the directory
   */
  public @NotNull Path getDataDir() {
    return this.dataDir;
  }

  /**
   * Check if an ItemStack is a blank captchacard.
   *
   * @param item the ItemStack to check
   * @return true if the ItemStack is a blank captchacard
   */
  @Contract("null -> false")
  public static boolean isBlankCaptcha(@Nullable ItemStack item) {
    return isCaptcha(item, pdc -> pdc.has(KEY_BLANK));
  }

  /**
   * Check if an ItemStack is a valid captchacard that has been used.
   *
   * @param item the ItemStack to check
   * @return true if the ItemStack is a captchacard
   */
  @Contract("null -> false")
  public static boolean isUsedCaptcha(@Nullable ItemStack item) {
    return isCaptcha(item, pdc -> pdc.has(KEY_HASH, PersistentDataType.STRING));
  }

  /**
   * Check if an ItemStack is a captchacard.
   *
   * @param item the ItemStack to check
   * @return true if the ItemStack is a captchacard
   */
  @Contract("null -> false")
  public static boolean isCaptcha(@Nullable ItemStack item) {
    return isCaptcha(item, pdc -> pdc.has(KEY_BLANK) || pdc.has(KEY_HASH, PersistentDataType.STRING));
  }

  /**
   * Check if an ItemStack is a captchacard.
   *
   * @param itemStack the ItemStack to check
   * @param predicate a predicate determining if the item's PersistentDataContainer is acceptable
   * @return true if the ItemStack is a captchacard
   */
  public static boolean isCaptcha(
      @Nullable ItemStack itemStack,
      @NotNull Predicate<PersistentDataContainer> predicate
  ) {
    // If the item is not a book or does not have meta, it cannot be a captcha.
    if (itemStack == null || itemStack.getType() != Material.BOOK || !itemStack.hasItemMeta()) {
      return false;
    }

    ItemMeta itemMeta = itemStack.getItemMeta();
    // Meta should not be null here, but just in case.
    if (itemMeta == null) {
      return false;
    }

    // Test for correct PDC content.
    return predicate.test(itemMeta.getPersistentDataContainer());
  }

  /**
   * Create a new blank captchacard.
   *
   * @return the new blank captchacard
   */
  @Contract("-> new")
  @SuppressWarnings("UnstableApiUsage")
  public @NotNull ItemStack newBlankCaptcha() {
    ItemStack itemStack = new ItemStack(Material.BOOK);
    itemStack.editMeta(meta -> {
      meta.displayName(lang.getComponent((String) null, Messages.ITEM_BLANK_NAME));
      Component component = lang.getComponent((String) null, Messages.ITEM_BLANK_DESCRIPTOR);
      if (component != null) {
        meta.lore(List.of(component));
      }
      meta.getPersistentDataContainer().set(KEY_BLANK, PersistentDataType.BYTE, (byte) 1);
      CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
      modelData.setStrings(List.of("captcha:blank"));
      meta.setCustomModelDataComponent(modelData);
    });
    return itemStack;
  }

  /**
   * Find the hash stored in the given captchacard.
   *
   * @param captcha the captchacard
   * @return the hash or {@code null}
   */
  @Contract("null -> null")
  public static @Nullable String getHashFromCaptcha(@Nullable ItemStack captcha) {
    AtomicReference<String> reference = new AtomicReference<>();
    isCaptcha(
        captcha,
        pdc -> {
          reference.set(pdc.get(KEY_HASH, PersistentDataType.STRING));
          return true;
        }
    );
    return reference.get();
  }

  /**
   * Check if an ItemStack cannot be turned into a captchacard. The only items that cannot be put into
   * a captcha are written books, items with inventories, and other captchas already at maximum depth.
   *
   * @param item the ItemStack to check
   * @return true if the ItemStack cannot be saved as a captchacard
   */
  public boolean canNotCaptcha(@Nullable ItemStack item) {
    return canNotCaptcha(item, true);
  }

  /**
   * Check if an ItemStack cannot be turned into a captchacard. The only items that cannot be put into
   * a captcha are written books, items with inventories, and other captchas already at maximum depth.
   *
   * @param item the ItemStack to check
   * @param requireMaxStacks true if the max stack size should be required
   * @return true if the ItemStack cannot be saved as a captchacard
   */
  public boolean canNotCaptcha(@Nullable ItemStack item, boolean requireMaxStacks) {
    if (item == null
        || item.getType() == Material.AIR
        // Book meta has high churn, no reason to allow creation of codes that will never be reused.
        || item.getType() == Material.WRITABLE_BOOK
        || item.getType() == Material.WRITTEN_BOOK
        // Knowledge book is specifically for usage, not for storage.
        || item.getType() == Material.KNOWLEDGE_BOOK
        // Only allow max stacks.
        || (requireMaxStacks && item.getAmount() != item.getMaxStackSize())) {
      return true;
    }
    if (item.hasItemMeta()) {
      ItemMeta meta = item.getItemMeta();
      if (meta instanceof BundleMeta
          || (meta instanceof BlockStateMeta stateMeta && stateMeta.getBlockState() instanceof InventoryHolder)) {
        return true;
      }
    }
    return getCaptchaDepth(item) >= MAX_CAPTCHA_DEPTH;
  }

  /**
   * Calculate captcha depth. This is the number of times a captcha must be undone to arrive at the
   * original stored item.
   *
   * @param item the captchacard
   * @return the captcha depth
   */
  public int getCaptchaDepth(@Nullable ItemStack item) {
    // If the item is not a used captcha, it has a depth of 0.
    if (!isUsedCaptcha(item)) {
      return 0;
    }

    int depth = 1;
    ItemStack newItem = getItemByCaptcha(item);
    while (isUsedCaptcha(newItem)) {
      // If the unpacked item is the same, it is invalid in some way.
      if (newItem.isSimilar(item)) {
        return depth;
      }
      // Increment depth and repeat unpack attempt on new item.
      ++depth;
      item = newItem;
      newItem = getItemByCaptcha(item);
    }
    return depth;
  }

  /**
   * Convert an ItemStack into a captchacard.
   *
   * @param item the ItemStack to convert
   * @return the captchacard representing by this ItemStack
   */
  public @Nullable ItemStack getCaptchaForItem(@NotNull ItemStack item) {
    item = item.clone();
    String itemHash = calculateHashForItem(item);
    this.save(itemHash, item);
    this.cache.put(itemHash, item);
    return getCaptchaForHash(itemHash);
  }

  /**
   * Get a captchacard for the specified hash.
   *
   * @param hash the hash
   * @return the captchacard
   */
  public @Nullable ItemStack getCaptchaForHash(@NotNull String hash) {
    ItemStack item = getItemByHash(hash);

    // Item not stored.
    if (item == null || item.getType() == Material.AIR) {
      return null;
    }

    // Get a new blank card to manipulate.
    ItemStack card = newBlankCaptcha();
    ItemMeta cardMeta = card.getItemMeta();
    if (cardMeta == null) {
      return null;
    }

    // Remove blank card tag and add hash.
    PersistentDataContainer dataContainer = cardMeta.getPersistentDataContainer();
    dataContainer.remove(KEY_BLANK);
    dataContainer.set(KEY_HASH, PersistentDataType.STRING, hash);

    // Add display elements for users.
    setCaptchaContentsDisplay(hash, item, cardMeta);

    card.setItemMeta(cardMeta);
    return card;
  }

  private void setCaptchaContentsDisplay(@NotNull String hash, @NotNull ItemStack item, @NotNull ItemMeta cardMeta) {
    Replacement[] replacements = getItemReplacements(hash, item);

    List<Component> cardLore = new ArrayList<>();
    Component component = lang.getComponent((String) null, Messages.ITEM_FILLED_DESCRIPTOR, replacements);
    if (component != null) {
      cardLore.add(component);
    }
    component = lang.getComponent((String) null, Messages.ITEM_FILLED_CONTENT, replacements);
    if (component != null) {
      cardLore.add(component);
    }
    cardMeta.lore(cardLore);

    Replacement[] replAndContent = new Replacement[replacements.length + 1];
    replAndContent[0] = Messages.ITEM_FILLED_CONTENT;
    System.arraycopy(replacements, 0, replAndContent, 1, replacements.length);
    cardMeta.displayName(lang.getComponent((String) null, Messages.ITEM_FILLED_NAME, replAndContent));

    // Handle texturing via CustomModelData.
    setCaptchaModelData(item, cardMeta);
  }

  private @NotNull Replacement @NotNull [] getItemReplacements(@NotNull String hash, @NotNull ItemStack item) {
    Replacement[] replacements = new Replacement[5];

    // Set final replacements to
    replacements[2] = new ItemNameReplacement(item);
    replacements[3] = new QuantityReplacement(item.getAmount());
    replacements[4] = new HashReplacement(hash.toUpperCase(Locale.ROOT));

    // If the item is not a used captcha, the root is the current item.
    if (!isUsedCaptcha(item)) {
      replacements[0] = new SimpleReplacement("rootContent", "{content}");
      replacements[1] = new SimpleReplacement("rootQuantity", "{quantity}");
      return replacements;
    }

    int rootAmount = item.getAmount();
    ItemStack rootItem = getItemByCaptcha(item);
    while (isUsedCaptcha(rootItem)) {
      // If the unpacked item is the same, it is invalid in some way.
      if (rootItem.isSimilar(item)) {
        break;
      }
      // For every stage, multiply by amount.
      rootAmount *= rootItem.getAmount();
      item = rootItem;
      rootItem = getItemByCaptcha(item);
    }

    if (rootItem != null && !rootItem.isSimilar(item)) {
      // If the fully-unpacked item exists and is valid, multiply by its amount.
      rootAmount *= rootItem.getAmount();
    } else {
      // Otherwise, set root to last valid unpacking phase.
      rootItem = item;
    }

    replacements[0] = new ItemNameReplacement("rootContent", rootItem);
    replacements[1] = new SimpleReplacement("rootQuantity", String.valueOf(rootAmount));

    return replacements;
  }

  @SuppressWarnings("UnstableApiUsage")
  private void setCaptchaModelData(@NotNull ItemStack item, @NotNull ItemMeta cardMeta) {
    AtomicBoolean blank = new AtomicBoolean();
    Predicate<PersistentDataContainer> consumer = pdc -> {
      if (pdc.has(KEY_BLANK)) {
        blank.set(true);
        return true;
      }
      return pdc.has(KEY_HASH, PersistentDataType.STRING);
    };
    List<String> modelStrings;
    if (isCaptcha(item, consumer)) {
      if (blank.get()) {
        // For blanks, use blank identifier.
        modelStrings = List.of("captcha:blanks");
      } else {
        // Otherwise, pull previous card type.
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
          modelStrings = itemMeta.getCustomModelDataComponent().getStrings();
        } else {
          // If card type is unavailable, fall through to unknown.
          modelStrings = List.of("captcha:unknown");
        }
      }
    } else {
      // Otherwise use the item type key.
      modelStrings = List.of("captcha:" + item.getType().key().asString());
    }

    CustomModelDataComponent modelData = cardMeta.getCustomModelDataComponent();
    modelData.setStrings(modelStrings);
    cardMeta.setCustomModelDataComponent(modelData);
  }

  /**
   * Convert a captchacard into an ItemStack.
   *
   * @param captcha the captchacard ItemStack
   * @return the ItemStack represented by this captchacard
   */
  @Contract("null -> null")
  public @Nullable ItemStack getItemByCaptcha(@Nullable ItemStack captcha) {
    if (captcha == null) {
      return null;
    }

    String hashFromCaptcha = getHashFromCaptcha(captcha);

    if (hashFromCaptcha != null) {
      ItemStack item = getItemByHash(hashFromCaptcha);
      if (item != null) {
        return item;
      }
    }

    captcha = captcha.clone();
    captcha.setAmount(1);
    return captcha;
  }

  /**
   * Get an item by hash. Uses cache, loading from disk as necessary.
   *
   * @param hash the hash to get an item for
   * @return the item or {@code null} if the item has not been saved
   */
  @Nullable
  public ItemStack getItemByHash(@NotNull String hash) {
    try {
      ItemStack itemStack = cache.get(hash);
      return itemStack != null ? new ItemStack(itemStack) : null;
    } catch (Exception e) {
      if (!(e.getCause() instanceof FileNotFoundException)) {
        logger.log(Level.WARNING, "Exception getting item by hash", e);
      }
      return null;
    }
  }

  /**
   * Calculate a hash for an item. Handles collisions by incrementing hash by 1.
   *
   * @param item the item to calculate a hash for
   * @return the calculated hash
   */
  public @NotNull String calculateHashForItem(@NotNull ItemStack item) {
    // Calculate MD5 of item NBT.
    BigInteger hash;
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      hash = new BigInteger(1, digest.digest(item.serializeAsBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    // Convert to base 36.
    String itemHash = hash.toString(Character.MAX_RADIX);

    // If a collision occurs, increment hash value.
    ItemStack captcha;
    while ((captcha = getItemByHash(itemHash)) != null && !captcha.equals(item)) {
      hash = hash.add(BigInteger.ONE);
      itemHash = hash.toString(Character.MAX_RADIX);
    }

    return itemHash;
  }

  private void save(@NotNull String hash, @NotNull ItemStack item) {
    try {
      Files.createDirectories(dataDir);
      Path hashPath = dataDir.resolve(hash + ".nbt");
      if (Files.exists(hashPath)) {
        return;
      }

      try (OutputStream out = Files.newOutputStream(hashPath)) {
        out.write(item.serializeAsBytes());
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error writing card contents", e);
    }
  }

}
