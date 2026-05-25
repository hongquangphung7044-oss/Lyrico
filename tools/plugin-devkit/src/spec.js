export const PLUGIN_API_VERSION = 1;
export const HOST_API_VERSION = 1;

export const CAPABILITIES = new Set([
  'searchSongs',
  'getLyrics',
  'searchCovers'
]);

export const CONFIG_FIELD_TYPES = new Set([
  'text',
  'password',
  'number',
  'switch',
  'dropdown',
  'textarea'
]);

export const METADATA_FIELD_TYPES = new Set([
  'text',
  'number',
  'date',
  'lyrics',
  'cover',
  'binary',
  'url'
]);

export const METADATA_WRITE_MODES = new Set([
  'DISABLED',
  'SUPPLEMENT',
  'OVERWRITE'
]);

export const METADATA_TARGETS = new Set([
  'TITLE',
  'ARTIST',
  'ALBUM',
  'ALBUM_ARTIST',
  'GENRE',
  'DATE',
  'TRACK_NUMBER',
  'DISC_NUMBER',
  'COMPOSER',
  'LYRICIST',
  'COMMENT',
  'LYRICS',
  'COVER',
  'LANGUAGE',
  'COPYRIGHT',
  'RATING',
  'REPLAY_GAIN_TRACK_GAIN',
  'REPLAY_GAIN_TRACK_PEAK',
  'REPLAY_GAIN_ALBUM_GAIN',
  'REPLAY_GAIN_ALBUM_PEAK',
  'REPLAY_GAIN_REFERENCE_LOUDNESS',
  'CUSTOM'
]);

export const HOST_APIS = new Set([
  'app.info',
  'app.userAgent',
  'runtime.info',
  'crypto.md5',
  'crypto.aesEcbPkcs5EncryptBase64',
  'crypto.aesEcbPkcs5EncryptHex',
  'crypto.aesEcbPkcs5DecryptBase64ToText',
  'base64.encodeText',
  'base64.decodeText',
  'base64.dropBytes',
  'base64.decodeBytes',
  'base64.encodeBytes',
  'bytes.xor',
  'bytes.xorBase64',
  'compression.inflateBytesToText',
  'compression.inflateBase64ToText',
  'http.getText',
  'http.postText',
  'http.postBytes',
  'http.get',
  'http.post',
  'http.getBytes',
  'http.postBytesResponse',
  'log.debug',
  'log.warn',
  'log.error'
]);

export const LIMITS = {
  manifestBytes: 128 * 1024,
  entryBytes: 1024 * 1024,
  singlePluginBytes: 5 * 1024 * 1024,
  zipEntries: 1000,
  zipDepth: 16
};
