-- Marketplace schema cleanup v1
-- Purpose: remove optional coupon persistence table now that coupons are served from an in-memory catalog.

DROP TABLE IF EXISTS marketplace_coupons;
