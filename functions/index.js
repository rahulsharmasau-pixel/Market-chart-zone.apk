const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-marketch-d6ed2d05-2a95-4493-827f-bc32c4ae02b3";
const CHANNEL_ID = "market_chart_zone_alerts";

async function dispatchCategoryPush({ title, body, category, targetId, imageUrl }) {
  const db = getFirestore(DATABASE_ID);
  const usersSnap = await db.collection("users").get();
  const tokens = [];

  usersSnap.forEach((doc) => {
    const data = doc.data();
    const muted = Array.isArray(data.mutedCategories) ? data.mutedCategories : [];
    const userTokens = Array.isArray(data.fcmTokens) ? data.fcmTokens : [];
    if (!muted.includes(category)) {
      userTokens.forEach((t) => {
        if (typeof t === "string" && t.length > 0) tokens.push(t);
      });
    }
  });

  if (tokens.length === 0) return;

  const chunks = [];
  for (let i = 0; i < tokens.length; i += 500) {
    chunks.push(tokens.slice(i, i + 500));
  }

  for (const batch of chunks) {
    const message = {
      tokens: batch,
      notification: {
        title,
        body,
        ...(imageUrl && imageUrl.startsWith("https://") ? { imageUrl } : {}),
      },
      data: {
        category,
        targetId: targetId || "",
        title,
        body,
        imageUrl: imageUrl || "",
      },
      android: {
        priority: "high",
        notification: {
          channelId: CHANNEL_ID,
          icon: "ic_notification_logo",
          sound: "default",
          defaultVibrateTimings: true,
          notificationPriority: "PRIORITY_MAX",
        },
      },
      apns: {
        headers: {
          "apns-priority": "10",
        },
        payload: {
          aps: {
            sound: "default",
            badge: 1,
            "mutable-content": 1,
          },
        },
      },
    };
    await getMessaging().sendEachForMulticast(message);
  }
}

exports.onVipTradeCreated = onDocumentCreated(
  { document: "vip_trades/{tradeId}", database: DATABASE_ID },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data();
    await dispatchCategoryPush({
      title: `VIP Signal: ${data.title}`,
      body: `${data.pair || ""} ${data.direction || ""} — Entry ${data.entryPrice || ""}`.trim(),
      category: "vip_trades",
      targetId: event.params.tradeId,
      imageUrl: data.imageUrl || "",
    });
  }
);

exports.onMarketAnalysisCreated = onDocumentCreated(
  { document: "market_analyses/{analysisId}", database: DATABASE_ID },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data();
    await dispatchCategoryPush({
      title: `Market Analysis: ${data.title}`,
      body: `${data.pair || ""} (${data.timeframe || ""}) — Tap to inspect key levels`,
      category: "market_analysis",
      targetId: event.params.analysisId,
      imageUrl: data.imageUrl || "",
    });
  }
);

exports.onAnnouncementCreated = onDocumentCreated(
  { document: "announcements/{announcementId}", database: DATABASE_ID },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data();
    await dispatchCategoryPush({
      title: data.title,
      body: data.message,
      category: data.category || "announcements",
      targetId: data.targetId || "",
      imageUrl: data.imageUrl || "",
    });
  }
);
